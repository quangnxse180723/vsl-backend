package com.vslbackend.service.impl;

import com.vslbackend.dto.request.user.ChangePasswordRequest;
import com.vslbackend.dto.request.user.UpdateUserRequest;
import com.vslbackend.dto.response.UserResponse;
import com.vslbackend.entity.User;
import com.vslbackend.repository.UserRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public UserResponse getCurrentUser() {
        User user = getCurrentUserEntity();

        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse updateProfile(UpdateUserRequest request) {
        User user = getCurrentUserEntity();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        userRepository.save(user);

        return UserResponse.builder()
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
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
        try {
            String fileName = UUID.randomUUID()
                    + "-" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload avatar failed", e);
        }
    }

    @Override
    public UserResponse updateAvatar(String avatarUrl) {
        User user = getCurrentUserEntity();

        user.setAvatarUrl(avatarUrl);

        userRepository.save(user);

        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Override
    public void deleteCurrentUser() {
        User user = getCurrentUserEntity();

        userRepository.delete(user);
    }

    private User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
