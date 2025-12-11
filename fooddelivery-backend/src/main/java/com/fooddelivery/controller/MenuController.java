package com.fooddelivery.controller;

import com.fooddelivery.dto.request.MenuItemRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.MenuItemResponse;
import com.fooddelivery.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1") // Spec said /api/v1/restaurants/...
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping(value = "/restaurants/{restaurantId}/menu-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @PathVariable String restaurantId,
            @RequestPart("item") @Valid MenuItemRequest item,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        
        MenuItemResponse response = menuService.createMenuItem(restaurantId, item, image);
        return new ResponseEntity<>(ApiResponse.success("Menu Item created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/restaurants/{restaurantId}/menu-items")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getMenuItems(@PathVariable String restaurantId) {
        List<MenuItemResponse> response = menuService.getMenuItems(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Menu Items fetched", response));
    }
}
