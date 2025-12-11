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

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    private String menuItemId; // Changed to String as per request but it's an ID reference snapshot
    private String name;
    private Integer quantity;
    private Double basePrice;
    private Double totalPrice;

    private String optionsJson;
}
