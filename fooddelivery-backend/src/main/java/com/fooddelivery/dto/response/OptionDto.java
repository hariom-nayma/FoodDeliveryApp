package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptionDto {
    private String id;
    private String label;
    private Double extraPrice;
}
