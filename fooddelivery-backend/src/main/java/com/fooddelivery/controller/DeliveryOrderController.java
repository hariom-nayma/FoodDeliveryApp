package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.entity.DeliveryAssignment;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.OrderStatus;
import com.fooddelivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/delivery/orders")
@RequiredArgsConstructor
public class DeliveryOrderController {

    private final OrderRepository orderRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final UserRepository userRepository;

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    private String getPartnerId(String userId) {
        return deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Not a delivery partner")).getId();
    }

    @GetMapping("/requests")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssignedOrderRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        // Mocking: Returns all READY_FOR_PICKUP orders that are NOT assigned yet
        // In real world, this would be specific assignments
        // For simplicity: Find all orders with status READY_FOR_PICKUP and
        // deliveryPartner = null

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> (o.getStatus() == OrderStatus.READY_FOR_PICKUP || o.getStatus() == OrderStatus.COOKING) && o.getDeliveryPartner() == null)
                .collect(Collectors.toList());

        List<Map<String, Object>> response = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("assignmentId", "assign_" + order.getId()); // Virtual assignment ID
            map.put("orderId", order.getId());
            map.put("restaurantName", order.getRestaurant().getName());
            // Mock locations
            map.put("pickupLocation", Map.of("lat", 28.62, "lng", 77.21));
            map.put("dropLocation", Map.of("lat", 28.61, "lng", 77.22));
            map.put("earnings", 35); // Fixed earnings
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Pending delivery requests", response));
    }

    @PostMapping("/requests/{assignmentId}/accept")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String assignmentId) {

        String orderId = assignmentId.replace("assign_", "");
        String userId = getUserId(userDetails);
        var partner = deliveryPartnerRepository.findByUserId(userId).orElseThrow();

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getDeliveryPartner() != null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Order already assigned", "ORDER_ALREADY_ASSIGNED"));
        }

        order.setDeliveryPartner(partner);
        order.setStatus(OrderStatus.ASSIGNED_TO_RIDER);
        orderRepository.save(order);

        // Create Assignment Record
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .order(order)
                .deliveryPartner(partner)
                .status("ACCEPTED")
                .assignedAt(LocalDateTime.now())
                .respondedAt(LocalDateTime.now())
                .build();
        deliveryAssignmentRepository.save(assignment);

        return ResponseEntity.ok(
                ApiResponse.success("Order accepted", Map.of("orderId", order.getId(), "status", "ASSIGNED_TO_RIDER")));
    }

    @PostMapping("/requests/{assignmentId}/reject")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Void>> rejectOrder(@PathVariable String assignmentId) {
        // Just ignore for now
        return ResponseEntity.ok(ApiResponse.success("Order rejected", null));
    }

    @PatchMapping("/{orderId}/picked-up")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markPickedUp(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);
        return ResponseEntity.ok(ApiResponse.success("Order picked up", Map.of("status", "PICKED_UP")));
    }

    @PatchMapping("/{orderId}/delivered")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markDelivered(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);
        return ResponseEntity.ok(ApiResponse.success("Order delivered", Map.of("status", "DELIVERED")));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        var partner = deliveryPartnerRepository.findByUserId(userId).orElseThrow();

        // Find orders by partner
        // Ideally add findByDeliveryPartnerId to OrderRepository
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getDeliveryPartner() != null && o.getDeliveryPartner().getId().equals(partner.getId()))
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());

        List<Map<String, Object>> res = orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", o.getId());
            map.put("earning", 35); // Fixed
            map.put("deliveredAt", o.getDeliveredAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Order history fetched", res));
    }

    @GetMapping("/earnings/daily")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyEarnings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> data = new HashMap<>();
        data.put("date", java.time.LocalDate.now());
        data.put("ordersCompleted", 5); // Mock
        data.put("totalEarnings", 175);
        data.put("incentives", 20);
        return ResponseEntity.ok(ApiResponse.success("Daily earnings fetched", data));
    }
}
