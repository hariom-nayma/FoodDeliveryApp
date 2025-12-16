package com.fooddelivery.dto.response;

import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private String id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private java.time.LocalDateTime premiumExpiry;
}
