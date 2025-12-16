package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {
    private String orderId;
    private String status;
    private LocalDateTime estimatedDeliveryTime;

    // Locations
    private Location userLocation;
    private Location restaurantLocation;
    private Location riderLocation;

    // Details for UI
    private String restaurantName;
    private String riderName;
    private String riderPhone;
    private String riderVehicleNumber;
    private String riderVehicleType;

    // Payment Info
    private Double totalAmount;
    private String paymentMethod;
    private String paymentStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Double latitude;
        private Double longitude;
        private String addressLabel; // e.g. "Home", "Restaurant Address"
    }
}
