package com.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    private Integer sortOrder;
    
    private Boolean active; // For updates
}
