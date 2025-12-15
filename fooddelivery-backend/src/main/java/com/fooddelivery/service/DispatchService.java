package com.fooddelivery.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.fooddelivery.entity.DeliveryAssignment;
import com.fooddelivery.entity.DeliveryPartner;
import com.fooddelivery.entity.Order;
import com.fooddelivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fooddelivery.dto.ScoredRider;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public void dispatchOrder(String orderId) {
        // Run in background
        log.info("Dispatching order: {}", orderId);
        scheduler.execute(() -> executeMatchingStep(orderId, INITIAL_SEARCH_RADIUS_KM, 1.0));
    }

    private void executeMatchingStep(String orderId, double radiusKm, double surgeMultiplier) {
        try {
            // Check limits
            if (radiusKm > MAX_SEARCH_RADIUS_KM) {
                if (surgeMultiplier == 1.0) {
                    log.info("Maximum radius reached. Retrying with Surge Pricing (10%)");
                    // Reset to max radius, apply surge
                    executeMatchingStep(orderId, MAX_SEARCH_RADIUS_KM, 1.10);
                    return;
                } else {
                    log.warn("Failed to match order {} even with surge. Giving up.", orderId);
                    // Optional: Mark order as MANUAL_ATTENTION
                    return;
                }
            }

            transactionTemplate.execute(status -> {
                log.info("Matching Step: Order={} Radius={} Surge={}", orderId, radiusKm, surgeMultiplier);
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order == null || order.getDeliveryPartner() != null) {
                    log.warn("Order not found or already assigned. Stopping.");
                    return null;
                }

                if (order.getRestaurant() == null || order.getRestaurant().getAddress() == null) {
                    log.error("Order Restaurant or Address missing");
                    return null;
                }

                // 1. Find nearby riders
                double lat = order.getRestaurant().getAddress().getLatitude();
                double lng = order.getRestaurant().getAddress().getLongitude();

                List<String> candidateIds = redisService.findNearbyRiders(lat, lng, radiusKm, 20);
                List<DeliveryPartner> candidates = deliveryPartnerRepository.findAllById(candidateIds);

                // 2. Score Riders (Filtering Rejections)
                List<ScoredRider> ranked = scoreAndRankCandidates(candidates, order, surgeMultiplier);

                if (ranked.isEmpty()) {
                    log.info("No valid candidates in {}km. Expanding...", radiusKm);
                    // Schedule next step immediately (or with small delay)
                    scheduler.execute(() -> executeMatchingStep(orderId, radiusKm + 3.0, surgeMultiplier));
                    return null;
                }

                // 3. Attempt Assignment
                attemptAssignment(ranked, orderId, radiusKm, surgeMultiplier);
                return null;
            });

        } catch (Exception e) {
            log.error("Error in executeMatchingStep: {}", e.getMessage(), e);
        }
    }

    private List<ScoredRider> scoreAndRankCandidates(List<DeliveryPartner> candidates, Order order, double surge) {
        double restLat = order.getRestaurant().getAddress().getLatitude();
        double restLng = order.getRestaurant().getAddress().getLongitude();

        // 1. Fetch Rejected/TimedOut IDs only if NOT in Surge mode
        // If Surge is applied (> 1.0), we give them a second chance with higher pay.
        java.util.Set<String> ignoreRiderIds = new java.util.HashSet<>();
            
        if (surge <= 1.001) {
            List<String> ignoreStatuses = List.of("REJECTED", "TIMED_OUT");
            List<DeliveryAssignment> failedAssignments = deliveryAssignmentRepository.findByOrderAndStatusIn(order,
                    ignoreStatuses);
            ignoreRiderIds = failedAssignments.stream()
                    .map(a -> a.getDeliveryPartner().getId())
                    .collect(Collectors.toSet());

            if (!ignoreRiderIds.isEmpty()) {
                log.debug("Filtering out {} riders who rejected/timed out: {}", ignoreRiderIds.size(), ignoreRiderIds);
            }
        } else {
             log.info("Surge applied ({}x). Including previously rejected/timed-out riders for retry.", surge);
        }

        // Need final variable for lambda
        final java.util.Set<String> finalIgnoreIds = ignoreRiderIds;

        return candidates.stream()
                .filter(rider -> !finalIgnoreIds.contains(rider.getId())) // Filter rejected (if any)
                .map(rider -> {
                    // Use ORS or Estimate for metrics
                    double distKm = calculateDistance(restLat, restLng, rider.getCurrentLatitude(),
                            rider.getCurrentLongitude());
                    double speedKmh = 30.0;
                    double durationMin = (distKm / speedKmh) * 60;

                    double score = scoringService.scoreRider(
                            rider, distKm, durationMin,
                            rider.getRatingAverage() != null ? rider.getRatingAverage() : 5.0,
                            rider.getTotalDeliveriesCompleted() != null ? 0 : 0);

                    return ScoredRider.builder()
                            .rider(rider)
                            .score(score)
                            .distanceKm(distKm)
                            .durationMin(durationMin)
                            .build();
                })
                .sorted(Comparator.comparing(ScoredRider::getScore).reversed()) // High score first
                .collect(Collectors.toList());
    }

    private void attemptAssignment(List<ScoredRider> candidates, String orderId, double radiusKm,
            double surgeMultiplier) {
        if (candidates.isEmpty()) {
            // All candidates in this radius/list exhausted. Expand radius.
            log.info("Exhausted candidates. Expanding radius.");
            scheduler.execute(() -> executeMatchingStep(orderId, radiusKm + 3.0, surgeMultiplier));
            return;
        }

        ScoredRider best = candidates.get(0);
        DeliveryPartner rider = best.getRider();

        // 1. Lock to check and assign
        String lockKey = "order_lock_" + orderId;
        if (!redisService.tryLock(lockKey, 5)) {
            // Retry this exact attempt in a moment? Or just skip to next?
            // Safer to skip to prevent infinite lock loops if something is stuck.
            attemptAssignment(candidates.subList(1, candidates.size()), orderId, radiusKm, surgeMultiplier);
            return;
        }

        boolean assigned = false;
        try {
            // 2. Critical Section: Check state and create PENDING assignment
            assigned = transactionTemplate.execute(status -> {
                Order freshOrder = orderRepository.findById(orderId).orElse(null);
                if (freshOrder == null || freshOrder.getDeliveryPartner() != null) {
                    return false; // Already assigned or invalid
                }

                DeliveryAssignment assignment = DeliveryAssignment.builder()
                        .order(freshOrder)
                        .deliveryPartner(rider)
                        .status("PENDING")
                        .assignedAt(LocalDateTime.now())
                        .build();
                deliveryAssignmentRepository.save(assignment);

                // Send Socket Event
                double payout = pricingService.calculatePayout(best.getDistanceKm(), best.getDurationMin(),
                        surgeMultiplier);
                Map<String, Object> payload = Map.of(
                        "assignmentId", assignment.getId().toString(),
                        "orderId", freshOrder.getId(),
                        "restaurantName", freshOrder.getRestaurant().getName(),
                        "earnings", payout,
                        "pickupLat", freshOrder.getRestaurant().getAddress().getLatitude(),
                        "pickupLng", freshOrder.getRestaurant().getAddress().getLongitude(),
                        "distanceKm", best.getDistanceKm(),
                        "eta", (int) best.getDurationMin());

                if (socketIOServer.getRoomOperations("rider_" + rider.getUserId()) != null) {
                    socketIOServer.getRoomOperations("rider_" + rider.getUserId())
                            .sendEvent("assignment_request", payload);
                }

                return true;
            });
        } finally {
            redisService.unlock(lockKey);
        }

        if (!assigned) {
            // Race condition or order taken. Stop this chain.
            return;
        }

        // 3. Schedule Async Timeout Check (15s)
        scheduler.schedule(() -> {
            transactionTemplate.execute(status -> {
                Order currentOrder = orderRepository.findById(orderId).orElse(null);
                if (currentOrder == null || currentOrder.getDeliveryPartner() != null)
                    return null;

                // Check PENDING assignments
                List<DeliveryAssignment> allAssignments = deliveryAssignmentRepository.findByOrder(currentOrder);
                for (DeliveryAssignment pa : allAssignments) {
                    // If we find the specific assignment we just made (or any pending), verify if
                    // it timed out.
                    if ("PENDING".equalsIgnoreCase(pa.getStatus()) &&
                            pa.getDeliveryPartner().getId().equals(rider.getId())) {

                        pa.setStatus("TIMED_OUT");
                        pa.setRespondedAt(LocalDateTime.now());
                        deliveryAssignmentRepository.save(pa);
                    }
                }

                // 4. Recurse to next candidate
                // We are in a new thread, call attemptAssignment with the rest of the list
                log.info("Timeout for rider {}. Trying next...", rider.getId());
                attemptAssignment(candidates.subList(1, candidates.size()), orderId, radiusKm, surgeMultiplier);
                return null;
            });
        }, 15, TimeUnit.SECONDS); // 15s Timeout
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
}
