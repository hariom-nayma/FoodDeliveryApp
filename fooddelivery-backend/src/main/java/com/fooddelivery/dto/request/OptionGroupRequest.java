package com.fooddelivery.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class OptionGroupRequest {
    private String name;
    private String type; // SINGLE_SELECT, MULTI_SELECT
    private boolean required;
    private List<OptionRequest> options;
}
