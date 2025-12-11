package com.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryPartnerRequest {
    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;
}
