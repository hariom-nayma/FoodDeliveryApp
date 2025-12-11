package com.fooddelivery.controller;

import com.fooddelivery.dto.request.AddressRequest;
import com.fooddelivery.dto.response.AddressResponse;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@AuthenticationPrincipal UserDetails userDetails,
                                                                   @RequestBody @Valid AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address added", addressService.addAddress(getUserId(userDetails), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@AuthenticationPrincipal UserDetails userDetails,
                                                                      @PathVariable String id,
                                                                      @RequestBody @Valid AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.updateAddress(getUserId(userDetails), id, request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched", addressService.getMyAddresses(getUserId(userDetails))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@AuthenticationPrincipal UserDetails userDetails,
                                                           @PathVariable String id) {
        addressService.deleteAddress(getUserId(userDetails), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }
}
