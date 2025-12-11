package com.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRequest {
    @NotBlank
    private String name;
    
    private String description;
    
    @NotBlank
    private String phone;
    
    @NotBlank
    private String email;
    
    @NotNull
    private List<String> cuisineTypes;
    
    @NotNull
    private RestaurantAddressDto address;
    
    @NotBlank
    private String openingTime;
    
    @NotBlank
    private String closingTime;
}
