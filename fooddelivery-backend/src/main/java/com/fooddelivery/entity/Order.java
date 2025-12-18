package com.fooddelivery.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "delivery_partner_id")
    private DeliveryPartner deliveryPartner;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String deliveryAddressJson;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(length = 32)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(length = 32)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(length = 32)
    private OrderType orderType;

    private Double subtotalAmount;
    private Double discountAmount;
    private Double taxAmount;
    private Double deliveryFee;
    private Double totalAmount;

    private Double riderEarning; // Amount payable to rider

    private String offerAppliedCode;

    private String paymentMethod; // COD or ONLINE

    // Razorpay Fields
    private String razorpayOrderId;
    private String paymentId;

    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime deliveredAt;

    @Builder.Default
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items = new ArrayList<>();
}
