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
@Table(name = "menu_item_option_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemOptionGroup extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    private String name; // e.g., Size, Add-ons

    private boolean isMultiSelect;
    private boolean isRequired;

    @OneToMany(mappedBy = "optionGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItemOption> options = new ArrayList<>();

    public void addOption(MenuItemOption option) {
        options.add(option);
        option.setOptionGroup(this);
    }
}
