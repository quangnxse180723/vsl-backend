package com.vslbackend.service.inter;

import com.vslbackend.dto.request.LoginRequest;
import com.vslbackend.dto.request.RefreshTokenRequest;
import com.vslbackend.dto.request.RegisterRequest;
import com.vslbackend.dto.response.AuthResponse;
import com.vslbackend.dto.response.UserResponse;

public interface AuthService {

    /** Dang ky tai khoan moi (role USER). */
    UserResponse register(RegisterRequest request);

    /** Dang nhap, tra ve access + refresh token. */
    AuthResponse login(LoginRequest request);

    /** Cap access token moi tu refresh token (kem xoay vong refresh token). */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /** Dang xuat - thu hoi refresh token. */
    void logout(RefreshTokenRequest request);

    /** Lay thong tin nguoi dung dang dang nhap. */
    UserResponse getCurrentUser(String email);
}
