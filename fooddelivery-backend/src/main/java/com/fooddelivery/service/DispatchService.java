package com.fooddelivery.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.fooddelivery.entity.DeliveryAssignment;
import com.fooddelivery.entity.DeliveryPartner;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.OrderStatus;
import com.fooddelivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fooddelivery.dto.ScoredRider;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final RedisService redisService;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderRepository orderRepository;
    private final SocketIOServer socketIOServer;
    private final ScoringService scoringService;
    private final PricingService pricingService;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private static final double INITIAL_SEARCH_RADIUS_KM = 3.0;
    private static final double MAX_SEARCH_RADIUS_KM = 12.0;
    private static final int MAX_ATTEMPTS = 8;

    public void dispatchOrder(String orderId) {
        log.info("DISPATCH: Starting dispatch for order {}", orderId);
        // Reset or init attempt counter if needed, but for now we just increment in
        // execute
        executeMatchingStep(orderId, INITIAL_SEARCH_RADIUS_KM, 1);
    }

    private void executeMatchingStep(String orderId, double radiusKm, int attempt) {
        try {
            // 0. Retry Safety Check
            if (attempt > MAX_ATTEMPTS) {
                log.warn("DISPATCH: Max attempts reached for order {}. Manual attention required.", orderId);
                return;
            }

            // 1. Calculate Surge based on Attempt
            // Attempt 1: 1.0, 2: 1.1, 3: 1.2, 4: 1.3 (Max)
            double surgeMultiplier = Math.min(1.3, 1.0 + ((attempt - 1) * 0.1));

            // Radius Logic: Expand if needed, based on attempt or explicit radius passing
            // For simplicity, we expand radius every 2 attempts
            double tempRadius = radiusKm;
            if (attempt > 2)
                tempRadius = Math.min(MAX_SEARCH_RADIUS_KM, radiusKm + 3.0);
            final double effectiveRadius = tempRadius;

            transactionTemplate.execute(status -> {
                log.info("DISPATCH_STEP: Order={} Attempt={} Radius={} Surge={}", orderId, attempt, effectiveRadius,
                        surgeMultiplier);

                Order order = orderRepository.findById(orderId).orElse(null);
                if (order == null || order.getDeliveryPartner() != null) {
                    log.info("DISPATCH: Order {} already assigned or missing. Stopping.", orderId);
                    return null;
                }

                if (order.getRestaurant() == null || order.getRestaurant().getAddress() == null) {
                    log.error("DISPATCH: Order {} missing location data", orderId);
                    return null;
                }

                // 2. Find Candidates
                double lat = order.getRestaurant().getAddress().getLatitude();
                double lng = order.getRestaurant().getAddress().getLongitude();

                List<String> candidateIds = redisService.findNearbyRiders(lat, lng, effectiveRadius, 30);
                log.info("DISPATCH_DEBUG: Found {} raw candidates in Redis: {}", candidateIds.size(), candidateIds);

                // 3. Filter Busy Riders (LOCK CHECK)
                // DEBUG: Force unlock on attempt 1 to clear stale locks
                if (attempt == 1) {
                    candidateIds.forEach(id -> {
                        if (redisService.isLocked("rider_busy_" + id)) {
                            log.warn("DISPATCH_DEBUG: Force unlocking rider {} for fresh dispatch", id);
                            redisService.unlock("rider_busy_" + id);
                        }
                    });
                }

                List<String> availableIds = candidateIds.stream()
                        .filter(id -> {
                            boolean locked = redisService.isLocked("rider_busy_" + id);
                            if (locked)
                                log.info("DISPATCH_DEBUG: Rider {} is BUSY/LOCKED", id);
                            return !locked;
                        })
                        .collect(Collectors.toList());

                log.info("DISPATCH_DEBUG: Available candidates after lock check: {}", availableIds.size());

                List<DeliveryPartner> candidates = deliveryPartnerRepository.findAllById(availableIds);

                // 4. Score & Rank
                List<ScoredRider> ranked = scoreAndRankCandidates(candidates, order, surgeMultiplier);
                log.info("DISPATCH_DEBUG: Ranked candidates: {}", ranked.size());

                if (ranked.isEmpty()) {
                    log.info("DISPATCH: No valid candidates found. Scheduling retry.");
                    // Exponential backoff or simple delay
                    scheduler.schedule(() -> executeMatchingStep(orderId, effectiveRadius, attempt + 1), 5,
                            TimeUnit.SECONDS);
                    return null;
                }

                // 5. Fairness: Pick from Top N
                // Don't always pick index 0. Pick random from top 3 to prevent race
                // conditions/spam
                int topN = Math.min(3, ranked.size());
                List<ScoredRider> topCandidates = ranked.subList(0, topN);
                Collections.shuffle(topCandidates);

                // Recursively attempt assignment
                final double currentRadius = effectiveRadius;
                attemptAssignment(topCandidates, orderId, currentRadius, attempt, surgeMultiplier);
                return null;
            });

        } catch (Exception e) {
            log.error("Error in executeMatchingStep: {}", e.getMessage(), e);
        }
    }

    private List<ScoredRider> scoreAndRankCandidates(List<DeliveryPartner> candidates, Order order, double surge) {
        double restLat = order.getRestaurant().getAddress().getLatitude();
        double restLng = order.getRestaurant().getAddress().getLongitude();

        // If not surging, filter out previously rejected
        java.util.Set<String> ignoreRiderIds = new java.util.HashSet<>();
        if (surge <= 1.05) {
            List<String> ignoreStatuses = List.of("REJECTED", "TIMED_OUT");
            List<DeliveryAssignment> failedAssignments = deliveryAssignmentRepository.findByOrderAndStatusIn(order,
                    ignoreStatuses);
            ignoreRiderIds = failedAssignments.stream().map(a -> a.getDeliveryPartner().getId())
                    .collect(Collectors.toSet());
        }

        final java.util.Set<String> finalIgnoreIds = ignoreRiderIds;

        return candidates.stream()
                .filter(rider -> !finalIgnoreIds.contains(rider.getId()))
                .map(rider -> {
                    double distKm = calculateDistance(restLat, restLng, rider.getCurrentLatitude(),
                            rider.getCurrentLongitude());
                    double durationMin = (distKm / 30.0) * 60;
                    double score = scoringService.scoreRider(rider, distKm, durationMin,
                            rider.getRatingAverage() != null ? rider.getRatingAverage() : 5.0, 0);

                    return ScoredRider.builder().rider(rider).score(score).distanceKm(distKm).durationMin(durationMin)
                            .build();
                })
                .sorted(Comparator.comparing(ScoredRider::getScore).reversed())
                .collect(Collectors.toList());
    }

    private void attemptAssignment(List<ScoredRider> candidates, String orderId, double radiusKm, int attempt,
            double surgeMultiplier) {
        if (candidates.isEmpty()) {
            // Should not happen given logic above, but safety
            scheduler.schedule(() -> executeMatchingStep(orderId, radiusKm, attempt + 1), 2, TimeUnit.SECONDS);
            return;
        }

        ScoredRider best = candidates.get(0);
        DeliveryPartner rider = best.getRider();
        String riderLockKey = "rider_busy_" + rider.getId();

        // 6. Lock Rider First (The "One Order" Rule)
        // Try to acquire lock for 45 mins (matches delivery time approx)
        String lockToken = UUID.randomUUID().toString();
        if (!redisService.tryLock(riderLockKey, lockToken, 45 * 60)) {
            log.info("DISPATCH: Rider {} became busy. Trying next candidate.", rider.getId());
            attemptAssignment(candidates.subList(1, candidates.size()), orderId, radiusKm, attempt, surgeMultiplier);
            return;
        }

        // 7. Order Lock (Prevent double assignment)
        String orderLockKey = "order_lock_" + orderId;
        String orderToken = UUID.randomUUID().toString();
        if (!redisService.tryLock(orderLockKey, orderToken, 10)) {
            // Rollback rider lock
            redisService.unlock(riderLockKey, lockToken);
            // Retry later
            scheduler.schedule(() -> executeMatchingStep(orderId, radiusKm, attempt), 1, TimeUnit.SECONDS);
            return;
        }

        boolean assigned = false;
        try {
            assigned = transactionTemplate.execute(status -> {
                Order freshOrder = orderRepository.findById(orderId).orElse(null);
                if (freshOrder == null || freshOrder.getDeliveryPartner() != null) {
                    return false;
                }

                double payout = pricingService.calculatePayout(best.getDistanceKm(), best.getDurationMin(),
                        surgeMultiplier);

                DeliveryAssignment assignment = DeliveryAssignment.builder()
                        .order(freshOrder)
                        .deliveryPartner(rider)
                        .status("PENDING")
                        .assignedAt(LocalDateTime.now())
                        .expectedEarning(payout)
                        .build();
                deliveryAssignmentRepository.save(assignment);

                // CRITICAL: Update Order Status so we don't dispatch again immediately
                freshOrder.setStatus(OrderStatus.ASSIGNED_TO_RIDER);
                orderRepository.save(freshOrder);

                Map<String, Object> payload = Map.of(
                        "assignmentId", assignment.getId().toString(),
                        "orderId", freshOrder.getId(),
                        "restaurantName", freshOrder.getRestaurant().getName(),
                        "earnings", payout,
                        "pickupLat", freshOrder.getRestaurant().getAddress().getLatitude(),
                        "pickupLng", freshOrder.getRestaurant().getAddress().getLongitude(),
                        "distanceKm", best.getDistanceKm(),
                        "eta", (int) best.getDurationMin(),
                        "surge", surgeMultiplier > 1.0);

                if (socketIOServer.getRoomOperations("rider_" + rider.getUserId()) != null) {
                    socketIOServer.getRoomOperations("rider_" + rider.getUserId())
                            .sendEvent("assignment_request", payload);
                }
                return true;
            });
        } finally {
            redisService.unlock(orderLockKey, orderToken);
            if (!assigned) {
                // If assignment failed (e.g. database error or validation), unlock rider
                redisService.unlock(riderLockKey, lockToken);
            }
        }

        if (!assigned)
            return;

        // 8. Wait for Response (Async Timeout)
        final String finalLockToken = lockToken;
        scheduler.schedule(() -> {
            log.info("DISPATCH_DEBUG: Executing timeout check for order {} rider {}", orderId, rider.getId());
            transactionTemplate.execute(status -> {
                // Re-verify assignment status
                // If still PENDING -> TIMEOUT
                // If TIMEOUT -> Unlock Rider & Retry

                // We need to fetch the assignment again
                DeliveryAssignment pa = deliveryAssignmentRepository.findByOrderAndStatusIn(
                        orderRepository.findById(orderId).orElseThrow(), List.of("PENDING"))
                        .stream().filter(a -> a.getDeliveryPartner().getId().equals(rider.getId()))
                        .findFirst().orElse(null);

                if (pa != null) {
                    log.info("DISPATCH: Timeout for rider {}. Re-dispatching.", rider.getId());
                    pa.setStatus("TIMED_OUT");
                    pa.setRespondedAt(LocalDateTime.now());
                    deliveryAssignmentRepository.save(pa);

                    // Revert Order Status so it can be picked up again?
                    // Or executeMatchingStep handles it.
                    // Ideally we should set it back to SEARCHING if we want consistency,
                    // but executeMatchingStep doesn't check status strictly (it checks
                    // deliveryPartner == null).
                    Order o = orderRepository.findById(orderId).orElse(null);
                    if (o != null) {
                        o.setStatus(OrderStatus.ACCEPTED); // Revert status
                        orderRepository.save(o);
                    }

                    // Release Rider Lock
                    boolean unlocked = redisService.unlock(riderLockKey, finalLockToken);
                    log.info("DISPATCH_DEBUG: Unlocked rider {}: {}", rider.getId(), unlocked);

                    // Trigger Next Attempt
                    executeMatchingStep(orderId, radiusKm, attempt + 1);
                } else {
                    log.info("DISPATCH_DEBUG: Assignment already handled (accepted/rejected) or not found.");
                }
                return null;
            });
        }, 15, TimeUnit.SECONDS);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;
        return dist;
    }

    public void sendOrderUpdate(String riderUserId, Map<String, Object> orderData) {
        if (socketIOServer.getRoomOperations("rider_" + riderUserId) != null) {
            socketIOServer.getRoomOperations("rider_" + riderUserId).sendEvent("order_update", orderData);
            log.info("Sent order update to rider_{}", riderUserId);
        }
    }

    public void releaseRiderLock(String riderId) {
        // Warning: This is a "force unlock". Ideally we should use the token,
        // but for delivery completion we assume we are the owner.
        // Or we can store the token in the Order entity if we want to be strict.
        // For now, force unlock is acceptable on delivery.
        redisService.unlock("rider_busy_" + riderId);
    }
}