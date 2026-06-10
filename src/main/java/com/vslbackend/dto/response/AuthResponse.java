package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Tra ve sau khi dang nhap / refresh thanh cong: day du access + refresh token. */
@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;

    @Builder.Default
    private final String tokenType = "Bearer";

    /** Thoi gian song cua access token (giay). */
    private final long expiresIn;

    private final UserResponse user;
}
