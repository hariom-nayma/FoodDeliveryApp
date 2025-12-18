package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.request.CreateOrderRequest;
import com.fooddelivery.dto.request.PaymentConfirmationRequest;
import com.fooddelivery.dto.response.OrderTrackingResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.OrderStatus;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> createOrder(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateOrderRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Order created. Complete payment to proceed.",
                        orderService.createOrder(getUserId(userDetails), request)));
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> confirmPayment(@PathVariable String id,
            @RequestBody PaymentConfirmationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed. Order placed successfully.",
                orderService.confirmPayment(id, request.getPaymentId(), request.getSignature())));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Order>> acceptOrder(@PathVariable String id) {
        return ResponseEntity
                .ok(ApiResponse.success("Order accepted", orderService.updateStatus(id, OrderStatus.ACCEPTED)));
    }

    @PatchMapping("/{id}/start-cooking")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Order>> startCooking(@PathVariable String id) {
        return ResponseEntity
                .ok(ApiResponse.success("Order marked as cooking", orderService.updateStatus(id, OrderStatus.COOKING)));
    }

    @PatchMapping("/{id}/ready")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Order>> readyForPickup(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Order ready for pickup",
                orderService.updateStatus(id, OrderStatus.READY_FOR_PICKUP)));
    }

    @PatchMapping("/{id}/picked-up")
    // @PreAuthorize("hasRole('DELIVERY_PARTNER')") // Uncomment when role exists
    public ResponseEntity<ApiResponse<Order>> pickedUp(@PathVariable String id) {
        return ResponseEntity
                .ok(ApiResponse.success("Order picked up", orderService.updateStatus(id, OrderStatus.PICKED_UP)));
    }

    @PatchMapping("/{id}/delivered")
    // @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Order>> delivered(@PathVariable String id) {
        return ResponseEntity
                .ok(ApiResponse.success("Order delivered", orderService.updateStatus(id, OrderStatus.DELIVERED)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(@PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .ok(ApiResponse.success("Order cancelled", orderService.cancelOrder(id, getUserId(userDetails))));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.fooddelivery.dto.response.PagedResponse<Order>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("My Orders",
                orderService.getMyOrders(getUserId(userDetails), page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Order details", orderService.getOrder(id)));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderTrackingResponse>>> getActiveOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .ok(ApiResponse.success("Active orders fetched", orderService.getActiveOrders(getUserId(userDetails))));
    }

    @GetMapping("/{id}/tracking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getTrackingDetails(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Tracking details", orderService.getTrackingDetails(id)));
    }
}
