package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartOptionResponse {
    private String groupName;
    private String optionSelected;
    private Double extraPrice;
}
