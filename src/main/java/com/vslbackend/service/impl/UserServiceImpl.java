package com.vslbackend.service.impl;

import com.vslbackend.dto.request.user.ChangePasswordRequest;
import com.vslbackend.dto.request.user.UpdateUserRequest;
import com.vslbackend.dto.response.UserResponse;
import com.vslbackend.entity.User;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.service.MinioService;
import com.vslbackend.service.inter.UserService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.vslbackend.dto.request.admin.AdminUpdateUserRequest;
import com.vslbackend.entity.UserStatus;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioService minioService;

    @Override
    public UserResponse getCurrentUser() {
        User user = getCurrentUserEntity();
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(UpdateUserRequest request) {
        User user = getCurrentUserEntity();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPasswordHash())) {

            throw new RuntimeException("Old password incorrect");
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        return minioService.uploadAvatar(file);
    }

    @Override
    public UserResponse updateAvatar(String avatarUrl) {
        User user = getCurrentUserEntity();

        user.setAvatarUrl(avatarUrl);

        userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Override
    public void deleteCurrentUser() {
        User user = getCurrentUserEntity();

        userRepository.delete(user);
    }

    // Admin APIs
    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse updateUserStatus(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Admin chi duoc bat/tat trang thai hoat dong, khong sua ten/vai tro/mat khau
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Override
    public UserResponse updateNotificationSettings(boolean emailNotificationsEnabled) {
        User user = getCurrentUserEntity();
        user.setEmailNotificationsEnabled(emailNotificationsEnabled);
        userRepository.save(user);
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .emailNotificationsEnabled(user.isEmailNotificationsEnabled())
                .build();
    }

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
