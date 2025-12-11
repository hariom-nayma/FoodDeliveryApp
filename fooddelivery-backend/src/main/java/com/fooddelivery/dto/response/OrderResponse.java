package com.fooddelivery.dto.response;

import com.fooddelivery.entity.OrderStatus;
import com.fooddelivery.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private Double totalAmount;
    private PricingResponse pricing;
    private LocalDateTime createdAt;

    // For Full Details
    private List<OrderItemResponse> items;
    private String restaurantName;
    private String deliveryAddress;

    // For Payment Phase
    private String paymentGatewayOrderId;
    private Double amountToPay;
}
