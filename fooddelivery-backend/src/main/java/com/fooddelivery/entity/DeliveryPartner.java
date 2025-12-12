package com.fooddelivery.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_partners")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartner extends BaseEntity {

    private String userId; // Link to User ID (String now)

    private String vehicleType;
    private String status; // PENDING, APPROVED, REJECTED

    private String drivingLicenseUrl;
    private String aadharCardUrl;
    private String vehicleRcUrl;

    private boolean isOnline;
    private Double currentLatitude;
    private Double currentLongitude;

    private Double ratingAverage;
    private Integer ratingCount;
    private Integer totalDeliveriesCompleted;

    private Double acceptanceRate; // 0.0 to 1.0
    private java.time.LocalDateTime lastAssignmentTime;
}
