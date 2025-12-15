package com.fooddelivery.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class RestaurantUpdateRequest {
    private String name;
    private String description;
    private String phone;
    private List<String> cuisineTypes;
    private String openingTime;
    private String closingTime;
    private String imageUrl;
}
