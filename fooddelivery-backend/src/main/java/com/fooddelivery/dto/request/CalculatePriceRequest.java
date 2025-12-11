package com.fooddelivery.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CalculatePriceRequest {
    private String restaurantId;
    private List<AddToCartRequest> items; // Reusing AddToCart which has itemId, qty, options
    private String offerCode;
    private String deliveryAddressId;
}
