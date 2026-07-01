package com.vslbackend.dto.request.admin;

import com.vslbackend.entity.Role;
import com.vslbackend.entity.UserStatus;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private String fullName;
    private Role role;
    private UserStatus status;
    private String password; // Optional
}
