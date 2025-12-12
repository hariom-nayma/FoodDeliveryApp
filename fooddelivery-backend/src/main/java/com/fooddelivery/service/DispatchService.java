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
            return;
        }
        
        ScoredRider best = candidates.get(0);
        DeliveryPartner rider = best.getRider();
        
        // 1. Lock to check and assign
        String lockKey = "order_lock_" + order.getId();
        if (!redisService.tryLock(lockKey, 5)) {
             // If locked, another thread is working on this order. 
             // We can abort this specific attempt stack.
             return;
        }
        
        boolean assigned = false;
        try {
            // 2. Critical Section: Check state and create PENDING assignment
             assigned = transactionTemplate.execute(status -> {
                Order freshOrder = orderRepository.findById(order.getId()).orElse(null);
                if (freshOrder == null || freshOrder.getDeliveryPartner() != null) {
                    return false; // Already assigned or invalid
                }
                
                // Double check if there is arguably a valid PENDING assignment? 
                // For simplicity, we assume previous ones are strictly timed out or rejected if we are here.
                
                DeliveryAssignment assignment = DeliveryAssignment.builder()
                        .order(freshOrder)
                        .deliveryPartner(rider)
                        .status("PENDING")
                        .assignedAt(LocalDateTime.now())
                        .build();
                deliveryAssignmentRepository.save(assignment);
                
                // Send Socket Event
                double payout = pricingService.calculatePayout(best.getDistanceKm(), best.getDurationMin(), surgeMultiplier);
                Map<String, Object> payload = Map.of(
                        "assignmentId", assignment.getId().toString(),
                        "orderId", freshOrder.getId(),
                        "restaurantName", freshOrder.getRestaurant().getName(),
                        "earnings", payout,
                        "pickupLat", freshOrder.getRestaurant().getAddress().getLatitude(),
                        "pickupLng", freshOrder.getRestaurant().getAddress().getLongitude(),
                        "distanceKm", best.getDistanceKm(),
                        "eta", (int) best.getDurationMin()
                );

                socketIOServer.getRoomOperations("rider_" + rider.getUserId())
                        .sendEvent("assignment_request", payload);
                        
                return true;
            });
        } finally {
            redisService.unlock(lockKey);
        }

        if (!assigned) {
            // If we failed to assign (e.g. order taken), stop.
            return;
        }

        // 3. Schedule Async Timeout Check (Outside lock)
        scheduler.schedule(() -> {
            transactionTemplate.execute(status -> {
                 Order currentOrder = orderRepository.findById(order.getId()).orElse(null);
                 if (currentOrder == null) return null;

                 // Check if THIS specific rider accepted
                 // We don't have the assignment ID here easily unless we pass it, 
                 // but checking order.getDeliveryPartner() is the source of truth.
                 if (currentOrder.getDeliveryPartner() != null) {
                     // Order is assigned. Success.
                     return null;
                 }
                 
                 // If not assigned, mark *any* PENDING assignments for this order as TIMED_OUT
                 List<DeliveryAssignment> allAssignments = deliveryAssignmentRepository.findByOrder(currentOrder);
                 for (DeliveryAssignment pa : allAssignments) {
                     if ("PENDING".equalsIgnoreCase(pa.getStatus())) {
                         pa.setStatus("TIMED_OUT");
                         pa.setRespondedAt(LocalDateTime.now());
                         deliveryAssignmentRepository.save(pa);
                     }
                 }
                 
                 // 4. Recurse to next candidate
                 // We are in a new thread, so stack is clean.
                 attemptAssignment(candidates.subList(1, candidates.size()), order, surgeMultiplier);
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
}