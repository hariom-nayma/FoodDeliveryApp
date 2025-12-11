package com.fooddelivery.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAddress {
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
}
