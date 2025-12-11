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
@Table(name = "offer_usages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferUsage extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
