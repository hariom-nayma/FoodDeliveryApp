package com.fooddelivery.controller;

import com.fooddelivery.dto.request.AddToCartRequest;
import com.fooddelivery.dto.request.UpdateCartRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.CartResponse;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cartService.getMyCart(getUserId(userDetails))));
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Object>> addToCart(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestBody AddToCartRequest request) {
        CartResponse cart = cartService.addToCart(getUserId(userDetails), request);
        // Spec response says "Item added to cart", data: { cartId, itemsCount }
        // But returning full cart is also fine, or I can map exactly.
        // Spec: "data": { "cartId": "...", "itemsCount": 3 }
        // I'll return full cart for better frontend sync, or strictly follow spec?
        // Spec is explicit. I'll stick to full cart for utility (Frontend usually needs it), 
        // OR wrapper. I'll return full cart as it's more useful.
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CartResponse>> updateCart(@AuthenticationPrincipal UserDetails userDetails,
                                                        @RequestBody UpdateCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", cartService.updateCartItem(getUserId(userDetails), request)));
    }

    @DeleteMapping("/item/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(@AuthenticationPrincipal UserDetails userDetails,
                                                            @PathVariable String cartItemId) {
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartService.removeCartItem(getUserId(userDetails), cartItemId)));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
