package com.fooddelivery.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class MenuItemRequest {
    private String name;
    private String description;
    private String categoryId;
    private Double basePrice;
    private String foodType; // VEG, NON_VEG
    private boolean available;
    private List<String> tags;
    private List<OptionGroupRequest> optionGroups;
}
