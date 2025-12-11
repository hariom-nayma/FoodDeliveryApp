package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MenuItemResponse {
    private String id;
    private String name;
    private String description;
    private Double basePrice;
    private String foodType; // VEG, NON_VEG
    private boolean available;
    private String imageUrl;
    private String categoryId;
    private List<OptionGroupDto> optionGroups;
}
