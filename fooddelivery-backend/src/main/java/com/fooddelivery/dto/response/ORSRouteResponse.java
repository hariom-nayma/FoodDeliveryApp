package com.fooddelivery.dto.response;

import lombok.Data;

@Data
public class ORSRouteResponse {
    private double distanceMeters;
    private double durationSeconds;
    private String polyline;
}
