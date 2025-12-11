package com.fooddelivery.dto.response;

import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserDto user;
}
