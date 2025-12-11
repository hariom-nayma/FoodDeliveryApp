package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private String addressId;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String label;
    private boolean isDefault;
}
