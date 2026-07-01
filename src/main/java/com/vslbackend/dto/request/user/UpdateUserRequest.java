package com.vslbackend.dto.request.user;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;
    private String email;
    private String fullName;
}
