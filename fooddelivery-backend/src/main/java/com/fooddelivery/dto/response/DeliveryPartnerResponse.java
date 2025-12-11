package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryPartnerResponse {
    private String id;
    private String userId;
    private String vehicleType;
    private String status;
    private String drivingLicenseUrl;
    private String aadharCardUrl;
    private String vehicleRcUrl;
    private boolean isOnline;
    private Double ratingAverage;
    private Integer totalDeliveriesCompleted;
    private LocalDateTime createdAt;
}
