package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.service.PaymentService;
import com.fooddelivery.service.UserService;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final UserRepository userRepository;

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiateSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        // Cost: 100 paise = ₹1
        String referenceId = "SUB_" + System.currentTimeMillis();
        String orderId = paymentService.createOrder(1.0, referenceId);
        
        return ResponseEntity.ok(ApiResponse.success("Subscription initiated", Map.of(
            "razorpayOrderId", orderId,
            "amount", "100", // paise
            "currency", "INR"
        )));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> verifySubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> payload) {
        
        String razorpayOrderId = payload.get("razorpayOrderId");
        String razorpayPaymentId = payload.get("razorpayPaymentId");
        String signature = payload.get("razorpaySignature");

        boolean valid = paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId, signature);
        if (!valid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid Payment Signature", "PAYMENT_FAILED"));
        }

        // Upgrade User
        userService.upgradeToPremium(getUserId(userDetails));

        return ResponseEntity.ok(ApiResponse.success("Subscription Activated", "PREMIUM_ACTIVATED"));
    }
}
