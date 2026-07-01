package com.vslbackend.dto.response;

import com.vslbackend.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Thong tin nguoi dung tra ve client (khong bao gio chua password). */
@Getter
@Builder
public class UserResponse {
    private final Long userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final String avatarUrl;
    private final Role role;
    private final com.vslbackend.entity.UserStatus status;
    private final LocalDateTime createdAt;
}
