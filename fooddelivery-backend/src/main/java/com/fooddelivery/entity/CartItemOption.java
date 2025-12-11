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
@Table(name = "cart_item_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemOption extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    private String optionGroupId;
    private String optionGroupName;
    
    private String optionId;
    private String optionName;
    private Double price; // Price at time of addition
}
