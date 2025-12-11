package com.fooddelivery.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantStatusUpdateRequest {
    private String status;
    private String reason; // For rejection
}
