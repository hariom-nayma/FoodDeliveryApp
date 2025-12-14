package com.fooddelivery.controller;

import com.fooddelivery.dto.request.DocumentUploadRequest;
import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.request.RestaurantStatusUpdateRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.RestaurantResponse;
import com.fooddelivery.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody RestaurantRequest request, Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return new ResponseEntity<>(ApiResponse.success("Restaurant created successfully",
                restaurantService.createRestaurant(request, email)), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocuments(
            @PathVariable String id,
            @RequestBody DocumentUploadRequest request,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        restaurantService.uploadDocuments(id, request, email);
        return ResponseEntity.ok(ApiResponse.success("Documents submitted successfully",
                Map.of("message", "Documents submitted successfully", "verificationStatus", "UNDER_REVIEW")));
    }

    @PatchMapping("/{id}/submit-for-review")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> submitForReview(@PathVariable String id,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(
                ApiResponse.success("Restaurant submitted for review", restaurantService.submitForReview(id, email)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getPendingRestaurants() {
        return ResponseEntity
                .ok(ApiResponse.success("Pending restaurants found", restaurantService.getPendingRestaurants()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> approveRestaurant(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant approved", restaurantService.approveRestaurant(id)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> rejectRestaurant(@PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                ApiResponse.success("Restaurant rejected", restaurantService.rejectRestaurant(id, body.get("reason"))));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateStatus(
            @PathVariable String id,
            @RequestBody RestaurantStatusUpdateRequest request,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.success("Restaurant status updated",
                restaurantService.updateStatus(id, request.getStatus(), email, isAdmin)));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getMyRestaurant(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        RestaurantResponse restaurant = restaurantService.getMyRestaurant(email);
        return restaurant != null ? ResponseEntity.ok(ApiResponse.success("Restaurant found", restaurant))
                : ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant found", restaurantService.getRestaurant(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> searchRestaurants(
            @RequestParam(required = false) String city) {
        if (city == null || city.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No restaurants found", List.of()));
        }
        return ResponseEntity.ok(ApiResponse.success("Restaurants found", restaurantService.searchRestaurants(city)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable String id,
            @Valid @RequestBody com.fooddelivery.dto.request.RestaurantUpdateRequest request,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated successfully",
                restaurantService.updateRestaurant(id, request, email)));
    }

    @GetMapping("/{id}/orders")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<List<com.fooddelivery.entity.Order>>> getRestaurantOrders(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Orders fetched", restaurantService.getOrders(id)));
    }
}
