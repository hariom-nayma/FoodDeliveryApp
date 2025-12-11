package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.request.CalculatePriceRequest;
import com.fooddelivery.dto.response.PricingResponse;
import com.fooddelivery.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<PricingResponse>> calculatePrice(@RequestBody CalculatePriceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Price calculated", pricingService.calculatePrice(request)));
    }
}
