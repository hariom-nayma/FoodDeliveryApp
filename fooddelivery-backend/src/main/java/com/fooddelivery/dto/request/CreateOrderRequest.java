package com.fooddelivery.dto.request;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private String deliveryAddressId;
    private String offerCode;
    private String paymentMethod; // ONLINE, COD
}
