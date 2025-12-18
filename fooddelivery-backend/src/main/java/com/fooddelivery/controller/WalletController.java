package com.fooddelivery.controller;

import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/partners/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{riderId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWalletSummary(@PathVariable String riderId) {
        Map<String, Object> summary = walletService.getWalletSummary(riderId);
        return ResponseEntity.ok(ApiResponse.success("Wallet summary",summary));
    }
}
