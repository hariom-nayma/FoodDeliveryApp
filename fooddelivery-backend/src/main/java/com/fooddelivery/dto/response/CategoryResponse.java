package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private String id;
    private String name;
    private String description;
    private Integer sortOrder;
    private boolean active;
}
