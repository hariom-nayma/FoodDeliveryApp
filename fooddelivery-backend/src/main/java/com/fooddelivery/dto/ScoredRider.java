package com.fooddelivery.dto;

import com.fooddelivery.entity.DeliveryPartner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredRider implements Comparable<ScoredRider> {
    private DeliveryPartner rider;
    private double score;
    private double distanceKm;
    private double durationMin;

    @Override
    public int compareTo(ScoredRider other) {
        // Higher score is better
        return Double.compare(other.score, this.score);
    }
}
