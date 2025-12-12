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
import org.springframework.stereotype.Service;

import com.fooddelivery.dto.ScoredRider;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
        System.out.println("Dispatching order: " + orderId);
        scheduler.execute(() -> startMatching(orderId));
    }

    private void startMatching(String orderId) {
        try {
            transactionTemplate.execute(status -> {
                System.out.println("Matching order: " + orderId);
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order == null) {
                    System.out.println("Order not found: " + orderId);
                    return null;
                }

                if (order.getRestaurant() == null || order.getRestaurant().getAddress() == null) {
                    System.out.println("Order Restaurant or Address missing");
                    return null;
                }

                double radiusKm = INITIAL_SEARCH_RADIUS_KM;
                boolean assigned = false;
                double surgeMultiplier = 1.0;
                int attempt = 0;

                while (!assigned && radiusKm <= MAX_SEARCH_RADIUS_KM) {
                    
                    // 1. Find nearby riders
                    double lat = order.getRestaurant().getAddress().getLatitude();
                    double lng = order.getRestaurant().getAddress().getLongitude();
                    System.out.println("Searching at: " + lat + ", " + lng + " Radius=" + radiusKm);
                    
                    List<String> candidateIds = redisService.findNearbyRiders(lat, lng, radiusKm, 20);
                    System.out.println("Found candidate IDs in Redis: " + candidateIds);
                    
                    List<DeliveryPartner> candidates = deliveryPartnerRepository.findAllById(candidateIds);
                    System.out.println("Found candidate Entities: " + candidates.size());
                    
                    // 2. Score Riders
                    List<ScoredRider> ranked = scoreAndRankCandidates(candidates, order, surgeMultiplier);
                    
                    // 3. Attempt Assignment (Recursive/Sequential)
                    if (!ranked.isEmpty()) {
                        attemptAssignment(ranked, order, surgeMultiplier);
                        return null; // Exit loop, let the recursion logic take over. 
                    }
                    
                    System.out.println("No candidates found in this radius, expanding radius: " + radiusKm);
                    radiusKm += 3.0;
                    surgeMultiplier += 0.1;
                    attempt++;
                }
                
                System.out.println("Exited loop. Assigned: " + assigned);
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Error in startMatching: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<ScoredRider> scoreAndRankCandidates(List<DeliveryPartner> candidates, Order order, double surge) {
        double restLat = order.getRestaurant().getAddress().getLatitude();
        double restLng = order.getRestaurant().getAddress().getLongitude();

        return candidates.stream().map(rider -> {
             // Use ORS or Estimate for metrics
             // For MVP speed, we use Haversine Estimate, ORS is expensive in loop
             // In Prod: Batch ORS call or use Haversine for filtering
            double distKm = calculateDistance(restLat, restLng, rider.getCurrentLatitude(), rider.getCurrentLongitude());
            double speedKmh = 30.0;
            double durationMin = (distKm / speedKmh) * 60;

            double score = scoringService.scoreRider(
                rider, distKm, durationMin, 
                rider.getRatingAverage() != null ? rider.getRatingAverage() : 5.0,
                rider.getTotalDeliveriesCompleted() != null ? 0 : 0 // Active orders not tracked in entity yet, assume 0
            );

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

    private void attemptAssignment(List<ScoredRider> candidates, Order order, double surgeMultiplier) {
        if (candidates.isEmpty()) {
            // No more candidates in this batch. 
            // In a full system, we would Trigger "Expand Radius" event here.
            return;
        }
        System.out.println("Attempting assignment for order: " + order.getId());

        ScoredRider best = candidates.get(0);
        DeliveryPartner rider = best.getRider();
        
        // Lock to prevent double assignment attempts if multiple threads run
        String lockKey = "order_lock_" + order.getId();
        if (!redisService.tryLock(lockKey, 15)) {
             // Failed to acquire lock, another process might be handling this order
             // For simplify, we just return or log. 
             // In recursive logic, this might be tricky if we want to retry later.
             // But if locked, it means we are ALREADY assigning.
             return;
        }
        
        sendAssignmentRequest(rider, order, best, surgeMultiplier);

        // Schedule check in 10s
        scheduler.schedule(() -> {
            // Need new transaction for check
            transactionTemplate.execute(status -> {
                // Check if Accepted
                boolean accepted = deliveryAssignmentRepository.existsByOrderAndStatus(order, "ACCEPTED"); // Simplified check
                // Note: deliveryAssignmentRepository needs to support `existsByOrderAndStatus` or similar.
                // Or check Order status directly.
                System.out.println("Assignment status for order: " + order.getId() + " is: " + accepted);
                
                Order currentOrder = orderRepository.findById(order.getId()).orElse(null);
                if (currentOrder != null && !"ASSIGNED_TO_RIDER".equals(currentOrder.getStatus().name())) {
                    // Not assigned yet (Rejected or Timeout)
                    // Mark this assignment as TIMEOUT if still pending?
                    
                    // Recurse: Try next candidate
                    attemptAssignment(candidates.subList(1, candidates.size()), order, surgeMultiplier);
                }
                return null;
            });
        }, 15, TimeUnit.SECONDS);
    }

    private void sendAssignmentRequest(DeliveryPartner rider, Order order, ScoredRider metrics, double surge) {
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .order(order)
                .deliveryPartner(rider)
                .status("PENDING")
                .assignedAt(LocalDateTime.now())
                .build();
        deliveryAssignmentRepository.save(assignment);
        System.out.println("Assignment created for order: " + order.getId());

        double payout = pricingService.calculatePayout(metrics.getDistanceKm(), metrics.getDurationMin(), surge);

        Map<String, Object> payload = Map.of(
                "assignmentId", assignment.getId().toString(),
                "orderId", order.getId(),
                "restaurantName", order.getRestaurant().getName(),
                "earnings", payout,
                "pickupLat", order.getRestaurant().getAddress().getLatitude(),
                "pickupLng", order.getRestaurant().getAddress().getLongitude(),
                "distanceKm", metrics.getDistanceKm(),
                "eta", (int) metrics.getDurationMin()
        );

        socketIOServer.getRoomOperations("rider_" + rider.getUserId())
                .sendEvent("assignment_request", payload);
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
}
