package com.vslbackend.controller;

import com.vslbackend.dto.request.user.ChangePasswordRequest;
import com.vslbackend.dto.request.user.UpdateAvatarRequest;
import com.vslbackend.dto.request.user.UpdateUserRequest;
import com.vslbackend.dto.response.UserResponse;
import com.vslbackend.service.inter.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest request) {

        userService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount() {

        userService.deleteCurrentUser();
        return ResponseEntity.ok("Account deleted successfully");
    }

    @PostMapping("/avatar/upload")
    public ResponseEntity<String> uploadAvatar(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                userService.uploadAvatar(file));
    }

    @PutMapping("/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @RequestBody UpdateAvatarRequest request) {

        return ResponseEntity.ok(
                userService.updateAvatar(request.getAvatarUrl()));
    }

    @PatchMapping("/me/notifications")
    public ResponseEntity<UserResponse> updateNotificationSettings(
            @RequestParam boolean emailNotificationsEnabled) {
        return ResponseEntity.ok(userService.updateNotificationSettings(emailNotificationsEnabled));
    }
}
