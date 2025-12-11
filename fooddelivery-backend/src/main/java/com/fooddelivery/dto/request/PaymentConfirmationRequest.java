package com.fooddelivery.dto.request;

import lombok.Data;

@Data
public class PaymentConfirmationRequest {
    private String paymentId;
    private String paymentGatewayOrderId;
    private String signature;
}
