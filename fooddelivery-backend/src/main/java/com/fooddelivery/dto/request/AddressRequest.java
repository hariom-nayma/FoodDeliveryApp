package com.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank
    private String line1;
    private String line2;
    @NotBlank
    private String city;
    @NotBlank
    private String state;
    @NotBlank
    private String postalCode; // matched to frontend 'postalCode'

    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;

    private String label; // Optional
    private boolean isDefault;
}
