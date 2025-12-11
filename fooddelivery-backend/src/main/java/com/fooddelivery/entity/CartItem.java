package com.fooddelivery.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    private Integer quantity;

    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItemOption> options = new ArrayList<>();

    private Double itemPrice; // Base price of Item
    private Double totalPrice; // (Base + Options) * Qty

    public void addOption(CartItemOption option) {
        options.add(option);
        option.setCartItem(this);
    }

    public void removeOption(CartItemOption option) {
        options.remove(option);
        option.setCartItem(null);
    }
}
