package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private String email;

    @ElementCollection
    @CollectionTable(name = "restaurant_cuisine_types", joinColumns = @JoinColumn(name = "restaurant_id"))
    @Column(name = "cuisine_type")
    private List<String> cuisineTypes;

    @Embedded
    private RestaurantAddress address;

    @Column(nullable = false)
    private String openingTime;

    @Column(nullable = false)
    private String closingTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RestaurantStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String gstNumber;
    private String fssaiNumber;
}
