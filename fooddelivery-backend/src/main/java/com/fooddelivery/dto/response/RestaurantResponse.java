package com.fooddelivery.dto.response;

import com.fooddelivery.dto.request.RestaurantAddressDto;
import com.fooddelivery.entity.RestaurantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {
    private String id;
    private String name;
    private String description;
    private String phone;
    private String email;
    private List<String> cuisineTypes;
    private RestaurantAddressDto address;
    private String openingTime;
    private String closingTime;
    private RestaurantStatus status;
    private String ownerId;
    private String ownerName;
    private String gstNumber;
    private String createdAt;
    private String imageUrl;
}
