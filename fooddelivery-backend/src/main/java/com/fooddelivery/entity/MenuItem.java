package com.fooddelivery.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    private String description;
    private Double basePrice;

    @Enumerated(EnumType.STRING)
    private FoodType foodType; // VEG, NON_VEG, VEGAN

    private boolean isAvailable;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private SpiceLevel spiceLevel; // MILD, MEDIUM, HOT

    private Integer prepTimeOverride; // nullable

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItemOptionGroup> optionGroups = new ArrayList<>();

    public void addOptionGroup(MenuItemOptionGroup group) {
        optionGroups.add(group);
        group.setMenuItem(this);
    }

    public void removeOptionGroup(MenuItemOptionGroup group) {
        optionGroups.remove(group);
        group.setMenuItem(null);
    }
}
