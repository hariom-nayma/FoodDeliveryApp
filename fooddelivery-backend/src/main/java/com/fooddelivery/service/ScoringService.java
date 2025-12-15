package com.fooddelivery.service;

import com.fooddelivery.entity.DeliveryPartner;
import org.springframework.stereotype.Service;

@Service
public class ScoringService {

    public double scoreRider(DeliveryPartner rider,
            double distanceToPickupKm,
            double durationToPickupMin,
            double riderRating,
            int activeOrders) {

        double score = 0;

        // Closest gets higher score (weight 5)
        // Avoid division by zero
        score += (1 / (1 + distanceToPickupKm)) * 5;

        // Pickup ETA (weight 5)
        score += (1 / (1 + durationToPickupMin)) * 5;

        // Rating factor (weight 2)
        score += riderRating * 2;

        // Penalize rider currently busy (weight 3)
        if (activeOrders > 0)
            score -= activeOrders * 3;

        return score;
    }
}
