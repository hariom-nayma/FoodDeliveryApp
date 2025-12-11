package com.fooddelivery.controller;

import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.CategoryResponse;
import com.fooddelivery.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @PathVariable String restaurantId,
            @RequestBody CategoryRequest request,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        CategoryResponse response = categoryService.createCategory(restaurantId, request, email);
        return new ResponseEntity<>(ApiResponse.success("Category created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @PathVariable String restaurantId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        // Public endpoint (conceptually), but maybe owners see inactive too. 
        // If owner calls it, they might want inactive. If customer, only active.
        // For now, controlled by param.
        List<CategoryResponse> response = categoryService.getCategories(restaurantId, includeInactive);
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", response));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable String restaurantId,
            @PathVariable String categoryId,
            @RequestBody CategoryRequest request,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        CategoryResponse response = categoryService.updateCategory(categoryId, restaurantId, request, email);
        return ResponseEntity.ok(ApiResponse.success("Category updated", response));
    }

    @PatchMapping("/{categoryId}/status")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleStatus(
            @PathVariable String restaurantId,
            @PathVariable String categoryId,
            @RequestBody Map<String, Boolean> statusMap,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        Boolean active = statusMap.get("active");
        if (active == null) throw new IllegalArgumentException("active field is required");
        
        CategoryResponse response = categoryService.toggleStatus(categoryId, restaurantId, active, email);
        return ResponseEntity.ok(ApiResponse.success("Category status updated", response));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable String restaurantId,
            @PathVariable String categoryId,
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        categoryService.deleteCategory(categoryId, restaurantId, email);
        return ResponseEntity.ok(ApiResponse.success("Category deleted"));
    }
}
