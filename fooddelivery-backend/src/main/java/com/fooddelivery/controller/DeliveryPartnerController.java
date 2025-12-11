package com.fooddelivery.controller;

import com.fooddelivery.dto.request.DeliveryPartnerRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.DeliveryPartnerResponse;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery-partners")
@RequiredArgsConstructor
public class DeliveryPartnerController {

    private final DeliveryPartnerService deliveryPartnerService;
    private final UserRepository userRepository;

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> register(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("data") @Valid DeliveryPartnerRequest request,
            @RequestPart("license") MultipartFile license,
            @RequestPart("aadhar") MultipartFile aadhar,
            @RequestPart("rc") MultipartFile rc) {

        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully",
                deliveryPartnerService.submitApplication(getUserId(userDetails), request, license, aadhar, rc)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DeliveryPartnerResponse>>> getPendingApplications() {
        return ResponseEntity
                .ok(ApiResponse.success("Pending applications", deliveryPartnerService.getPendingApplications()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> approveApplication(@PathVariable String id) {
        return ResponseEntity
                .ok(ApiResponse.success("Application approved", deliveryPartnerService.approveApplication(id)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> rejectApplication(@PathVariable String id) {
        return ResponseEntity
                .ok(ApiResponse.success("Application rejected", deliveryPartnerService.rejectApplication(id)));
    }

    @PatchMapping("/status/online")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> goOnline(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody java.util.Map<String, Double> location) {
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Partner is now online",
                deliveryPartnerService.toggleOnlineStatus(userId, true, location.get("latitude"),
                        location.get("longitude"))));
    }

    @PatchMapping("/status/offline")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> goOffline(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Partner is now offline",
                deliveryPartnerService.toggleOnlineStatus(userId, false, null, null)));
    }

    @PatchMapping("/location")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody java.util.Map<String, Double> location) {
        String userId = getUserId(userDetails);
        deliveryPartnerService.updateLocation(userId, location.get("latitude"), location.get("longitude"));
        return ResponseEntity.ok(ApiResponse.success("Location updated", null));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", deliveryPartnerService.getProfile(userId)));
    }
}
