package com.fooddelivery.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateCartRequest {
    private String cartItemId;
    private Integer quantity; 
    private List<CartOptionRequest> options; // Optional
}
