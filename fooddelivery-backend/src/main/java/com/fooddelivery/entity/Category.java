package com.fooddelivery.entity;

import jakarta.persistence.Column;
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
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    @Builder.Default
    private boolean active = true;
}
