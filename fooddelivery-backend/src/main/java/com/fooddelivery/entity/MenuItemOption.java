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
@Table(name = "menu_item_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemOption extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "option_group_id", nullable = false)
    private MenuItemOptionGroup optionGroup;

    private String label; // Cheese, Coke
    private Double extraPrice;
}
