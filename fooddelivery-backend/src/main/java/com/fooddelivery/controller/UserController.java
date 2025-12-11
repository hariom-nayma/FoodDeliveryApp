package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.request.AddressDto;
import com.fooddelivery.dto.response.UserDto;
import com.fooddelivery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getUserProfile() {
        return ResponseEntity.ok(ApiResponse.success("User profile verified", userService.getUserProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateUserProfile(@RequestBody UserDto request) {
        return ResponseEntity
                .ok(ApiResponse.success("Profile updated successfully", userService.updateUserProfile(request)));
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<AddressDto>>> getAddresses() {
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched successfully", userService.getAddresses()));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<AddressDto>> addAddress(@RequestBody AddressDto request) {
        return ResponseEntity.ok(ApiResponse.success("Address added successfully", userService.addAddress(request)));
    }

    @PutMapping("/me/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressDto>> updateAddress(@PathVariable String id,
            @RequestBody AddressDto request) {
        return ResponseEntity
                .ok(ApiResponse.success("Address updated successfully", userService.updateAddress(id, request)));
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(@PathVariable String id) {
        userService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", "Address deleted successfully"));
    }
}
