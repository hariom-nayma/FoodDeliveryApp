package com.fooddelivery.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignment extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "delivery_partner_id", nullable = false)
    private DeliveryPartner deliveryPartner;

    private String status;

    private LocalDateTime assignedAt;
    private LocalDateTime respondedAt;
}
