package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CartItemResponse {
    private String cartItemId;
    private String itemId; // menuItemId
    private String name;
    private Integer quantity;
    private Double basePrice;
    private Double finalPrice; // Unit price with options? Or total line price? Spec says "finalPrice: 560" for qty 2 @ 280. So line total.
    private List<CartOptionResponse> options;
}
