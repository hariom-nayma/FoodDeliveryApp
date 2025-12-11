package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private String name;
    private Integer quantity;
    private Double price;
    private List<String> options;
}
