package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResponse {
    private Double subtotal;
    private Double discount;
    private Double tax;
    private Double deliveryFee;
    private Double total;
    private String offerApplied;
    private Integer etaMinutes;
}
