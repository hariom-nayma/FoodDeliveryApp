package com.fooddelivery.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressDto {
    private String id; // Optional for creation, required for response
    private String label;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private boolean isDefault;
}
