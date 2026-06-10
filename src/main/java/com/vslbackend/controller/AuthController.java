package com.vslbackend.controller;

import com.vslbackend.dto.request.LoginRequest;
import com.vslbackend.dto.request.RefreshTokenRequest;
import com.vslbackend.dto.request.RegisterRequest;
import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.AuthResponse;
import com.vslbackend.dto.response.UserResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Dang ky thanh cong", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(ApiResponse.of("Dang nhap thanh cong", auth));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse auth = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.of("Lam moi token thanh cong", auth));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.of("Dang xuat thanh cong"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserResponse user = authService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.of("Thong tin tai khoan", user));
    }
}
