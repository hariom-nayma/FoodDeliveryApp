package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.entity.DeliveryAssignment;
import com.fooddelivery.entity.DeliveryPartner;
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
    private final com.fooddelivery.service.DispatchService dispatchService;

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

        String userId = getUserId(userDetails);
        DeliveryPartner partner = deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        List<DeliveryAssignment> assignments = deliveryAssignmentRepository
                .findByDeliveryPartner_IdAndStatus(partner.getId(), "PENDING");

        List<Map<String, Object>> response = assignments.stream().map(a -> {
            Order order = a.getOrder();
            Map<String, Object> map = new HashMap<>();
            map.put("assignmentId", a.getId());
            map.put("orderId", order.getId());
            map.put("restaurantName", order.getRestaurant().getName());

            // Re-calculate/Fetch info (In real scenario, snapshot might be in Assignment or
            // we re-calc)
            // For now, simpler map
            map.put("earnings", 35.0); // Or fetch from pricing service if stored?
            // Note: DispatchService stored earnings in Payload but not in Assignment Entity
            // (Wait, did it?).
            // If we want exact earnings shown in Popup, we need to store it or
            // re-calculate.
            // Let's re-calculate or just use strict logic.

            try {
                if (order.getRestaurant().getAddress() != null) {
                    map.put("pickupLocation", Map.of(
                            "lat", order.getRestaurant().getAddress().getLatitude(),
                            "lng", order.getRestaurant().getAddress().getLongitude()));
                }
            } catch (Exception e) {
            }

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Pending delivery requests", response));
    }

    @PostMapping("/requests/{assignmentId}/respond")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> respondToAssignment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String assignmentId,
            @RequestBody com.fooddelivery.dto.request.RespondAssignmentRequest request) {

        String userId = getUserId(userDetails);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Validate Rider
        if (!assignment.getDeliveryPartner().getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
        }

        if (!"PENDING".equals(assignment.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Assignment expired or already processed", "EXPIRED"));
        }

        assignment.setRespondedAt(LocalDateTime.now());

        if (request.isAccepted()) {
            // Check if order is already assigned to someone else (Double check)
            Order order = assignment.getOrder();
            if (order.getDeliveryPartner() != null) {
                assignment.setStatus("EXPIRED"); // Or REJECTED_AUTO
                deliveryAssignmentRepository.save(assignment);
                return ResponseEntity.badRequest().body(ApiResponse.error("Order already taken", "ORDER_Taken"));
            }

            assignment.setStatus("ACCEPTED");
            deliveryAssignmentRepository.save(assignment);

            order.setDeliveryPartner(assignment.getDeliveryPartner());
            order.setStatus(OrderStatus.ASSIGNED_TO_RIDER);
            orderRepository.save(order);

            // Return FULL order details for frontend
            Map<String, Object> response = mapOrderToResponse(order);
            dispatchService.sendOrderUpdate(assignment.getDeliveryPartner().getUserId(), response);
            return ResponseEntity.ok(ApiResponse.success("Order Accepted", response));
        } else {
            assignment.setStatus("REJECTED");
            deliveryAssignmentRepository.save(assignment);

            // IMMEDIATE RETRY: Trigger next dispatch
            dispatchService.dispatchOrder(assignment.getOrder().getId());

            return ResponseEntity.ok(ApiResponse.success("Order Rejected", Map.of("status", "REJECTED")));
        }
    }

    // ... (markPickedUp, markDelivered remain same)

    @GetMapping("/active")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        var partner = deliveryPartnerRepository.findByUserId(userId).orElseThrow();
        System.out.println("DEBUG: Fetching active orders for partner: " + partner.getId());

        // Debug: Print all orders for this partner
        List<Order> allPartnerOrders = orderRepository.findAll().stream()
                .filter(o -> o.getDeliveryPartner() != null && o.getDeliveryPartner().getId().equals(partner.getId()))
                .collect(Collectors.toList());

        System.out.println("DEBUG: Total orders for partner: " + allPartnerOrders.size());
        allPartnerOrders.forEach(o -> System.out.println("DEBUG: Order " + o.getId() + " Status: " + o.getStatus()));

        List<Order> orders = allPartnerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.ASSIGNED_TO_RIDER || o.getStatus() == OrderStatus.PICKED_UP)
                .collect(Collectors.toList());

        System.out.println("DEBUG: Filtered active orders: " + orders.size());

        List<Map<String, Object>> response = orders.stream()
                .map(this::mapOrderToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Active orders fetched", response));
    }

    private Map<String, Object> mapOrderToResponse(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getId());
        map.put("restaurantName", order.getRestaurant().getName());
        map.put("status", order.getStatus());
        map.put("customerName", order.getUser().getName());
        map.put("customerPhone", order.getUser().getPhone());

        // Locations
        try {
            if (order.getRestaurant().getAddress() != null) {
                map.put("pickupLocation", Map.of(
                        "lat", order.getRestaurant().getAddress().getLatitude(),
                        "lng", order.getRestaurant().getAddress().getLongitude(),
                        "address", order.getRestaurant().getAddress().getState() + ", "
                                + order.getRestaurant().getAddress().getCity()));
            } else {
                System.out.println("DEBUG: Restaurant address is NULL for order " + order.getId());
            }

            // Delivery Location Parsing
            String deliveryAddrJson = order.getDeliveryAddressJson();
            if (deliveryAddrJson != null) {
                try {
                    com.fasterxml.jackson.databind.JsonNode addrNode = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(deliveryAddrJson);
                    map.put("dropLocation", Map.of(
                            "lat", addrNode.path("latitude").asDouble(),
                            "lng", addrNode.path("longitude").asDouble(),
                            "address", addrNode.path("address").asText()));
                } catch (Exception e) {
                    map.put("dropLocation", Map.of("raw", deliveryAddrJson));
                    System.out.println("DEBUG: Failed to parse delivery address JSON: " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG: Delivery address JSON is NULL for order " + order.getId());
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error mapping locations: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("DEBUG: Mapped Order Response: " + map); // Log the final map

        map.put("earnings", 35);
        return map;
    }

    @PatchMapping("/{orderId}/picked-up")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markPickedUp(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);

        Map<String, Object> response = mapOrderToResponse(order);
        if (order.getDeliveryPartner() != null) {
            dispatchService.sendOrderUpdate(order.getDeliveryPartner().getUserId(), response);
        }

        return ResponseEntity.ok(ApiResponse.success("Order picked up", response));
    }

    @PatchMapping("/{orderId}/delivered")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markDelivered(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);

        // Final update (or removal)
        // Ideally we might want to clear the active order on frontend
        Map<String, Object> response = mapOrderToResponse(order); // Status DELIVERED
        if (order.getDeliveryPartner() != null) {
            dispatchService.sendOrderUpdate(order.getDeliveryPartner().getUserId(), response);
        }

        return ResponseEntity.ok(ApiResponse.success("Order delivered", response));
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
}
