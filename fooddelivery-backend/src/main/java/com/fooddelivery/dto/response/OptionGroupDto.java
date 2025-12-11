package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OptionGroupDto {
    private String id;
    private String name;
    private boolean multiSelect;
    private boolean required;
    private List<OptionDto> options;
}
