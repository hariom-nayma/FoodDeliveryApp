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
@Table(name = "addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String label; // e.g. Home, Work
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private boolean isDefault;
}
