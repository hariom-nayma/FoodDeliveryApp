package com.fooddelivery.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class AddToCartRequest {
    private String restaurantId;
    private String itemId;
    private Integer quantity;
    private List<CartOptionRequest> options;
}


