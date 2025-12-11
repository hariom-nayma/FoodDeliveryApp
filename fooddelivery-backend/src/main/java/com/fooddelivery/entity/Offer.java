package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String code;

    private String title;
    private String description;

    // Type: FLAT, PERCENTAGE, DELIVERY_DISCOUNT
    private String discountType;

    private Double discountValue;
    private Double maxDiscountAmount;
    private Double minOrderAmount;

    private Integer usageLimitPerUser;
    private Integer totalUsageLimit;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String applicableRestaurantId; // Nullable, null means platform-wide (UUID string)

    private boolean active;
}
