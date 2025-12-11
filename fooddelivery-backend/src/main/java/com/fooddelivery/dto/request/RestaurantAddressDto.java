package com.fooddelivery.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantAddressDto {
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
}
