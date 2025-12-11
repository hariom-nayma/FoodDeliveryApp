package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private String cartId;
    private String restaurantId;
    private List<CartItemResponse> items;
    private Double subtotal;
    private Double tax;
    private Double deliveryFee;
    private Double total;
}
