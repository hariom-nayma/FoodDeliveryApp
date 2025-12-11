package com.fooddelivery.dto.request;

import com.fooddelivery.entity.OtpType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    private String authToken;
    
    private OtpType type;
}
