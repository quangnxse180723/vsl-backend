package com.vslbackend.dto.request.admin;

import com.vslbackend.entity.Role;
import com.vslbackend.entity.UserStatus;
import lombok.Data;

@Data
public class AdminCreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private Role role;
    private UserStatus status;
}
