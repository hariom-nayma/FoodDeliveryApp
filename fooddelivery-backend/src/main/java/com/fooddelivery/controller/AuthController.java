package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.request.LoginRequest;
import com.fooddelivery.dto.request.RegisterRequest;
import com.fooddelivery.dto.response.AuthResponse;
import com.fooddelivery.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.fooddelivery.dto.request.VerifyOtpRequest;
import com.fooddelivery.dto.response.OtpResponseDto;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OtpResponseDto>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", authService.register(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Verification successful", authService.verifyOtp(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<OtpResponseDto>> forgotPassword(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", authService.forgotPassword(email)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody com.fooddelivery.dto.request.RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody com.fooddelivery.dto.request.RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "Logged out successfully"));
    }
}
