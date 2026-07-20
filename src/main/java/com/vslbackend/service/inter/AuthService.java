package com.vslbackend.service.inter;

import com.vslbackend.dto.request.LoginRequest;
import com.vslbackend.dto.request.RefreshTokenRequest;
import com.vslbackend.dto.request.RegisterRequest;
import com.vslbackend.dto.response.AuthResponse;
import com.vslbackend.dto.response.UserResponse;

public interface AuthService {

    /** Dang ky tai khoan moi (role USER). */
    UserResponse register(RegisterRequest request);

    /** Yeu cau gui ma OTP xac thuc email khi dang ky. */
    void sendRegisterOtp(com.vslbackend.dto.request.auth.SendRegisterOtpRequest request);

    /** Dang nhap, tra ve access + refresh token. */
    AuthResponse login(LoginRequest request);

    /** Cap access token moi tu refresh token (kem xoay vong refresh token). */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /** Dang xuat - thu hoi refresh token. */
    void logout(RefreshTokenRequest request);

    /** Lay thong tin nguoi dung dang dang nhap. */
    UserResponse getCurrentUser(String email);

    /** Yeu cau gui ma OTP de khoi phuc mat khau. */
    void forgotPassword(com.vslbackend.dto.request.auth.ForgotPasswordRequest request);

    /** Kiem tra ma OTP hop le. */
    void verifyOtp(com.vslbackend.dto.request.auth.VerifyOtpRequest request);

    /** Dat lai mat khau moi. */
    void resetPassword(com.vslbackend.dto.request.auth.ResetPasswordRequest request);
}
