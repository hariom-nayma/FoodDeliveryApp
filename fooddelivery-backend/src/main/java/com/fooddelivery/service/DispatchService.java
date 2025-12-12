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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final RedisService redisService;
    private final OpenRouteService openRouteService;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderRepository orderRepository;
    private final SocketIOServer socketIOServer;

    private static final double SEARCH_RADIUS_KM = 3.0;

    public void dispatchOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (order.getRestaurant().getAddress() == null)
            return;

        double restLat = order.getRestaurant().getAddress().getLatitude();
        double restLng = order.getRestaurant().getAddress().getLongitude();

        // 1. Find nearby riders
        List<String> riderIds = redisService.findNearbyRiders(restLat, restLng, SEARCH_RADIUS_KM, 10);

        if (riderIds.isEmpty()) {
            // Handle fallback (expand radius - future)
            return;
        }

        // 2. Score Riders (Simplified: just distance for now)
        // In real app: fetch rider stats, rating, etc.
        List<DeliveryPartner> riders = deliveryPartnerRepository.findAllById(riderIds);

        // 3. Sort by score (mock: simple closest first as Redis returned sorted by
        // dist)
        // We'll trust Redis for distance sort.

        // 4. Send Request to Best Rider (First one)
        // ideally we loop, but for MVP we try the first one.
        if (!riders.isEmpty()) {
            sendAssignmentRequest(riders.get(0), order);
        }
    }

    private void sendAssignmentRequest(DeliveryPartner rider, Order order) {
        // Create Assignment Record
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .order(order)
                .deliveryPartner(rider)
                .status("PENDING")
                .assignedAt(LocalDateTime.now())
                .build();
        deliveryAssignmentRepository.save(assignment);

        // Prepare Payload
        Map<String, Object> payload = Map.of(
                "assignmentId", "assign_" + assignment.getId(), // or use real ID
                "orderId", order.getId(),
                "restaurantName", order.getRestaurant().getName(),
                "earnings", 40.0, // Dynamic pricing engine
                "eta", 10 // from ORS
        );

        // Send via Socket.IO
        // Assuming client is joined room "rider_{userId}"
        socketIOServer.getRoomOperations("rider_" + rider.getUserId())
                .sendEvent("assignment_request", payload);

        // Timeout handling would be scheduled here (ScheduledExecutorService)
    }
}
