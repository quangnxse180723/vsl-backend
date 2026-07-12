package com.vslbackend.service.inter;

import com.vslbackend.dto.request.user.ChangePasswordRequest;
import com.vslbackend.dto.request.user.UpdateUserRequest;
import com.vslbackend.dto.response.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.vslbackend.dto.request.admin.AdminUpdateUserRequest;

@Service
public interface UserService {

    UserResponse getCurrentUser();

    UserResponse updateProfile(UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);

    String uploadAvatar(MultipartFile file);

    UserResponse updateAvatar(String avatarUrl);

    void deleteCurrentUser();

    // Admin APIs
    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(Long id);

    // Chi cap nhat trang thai hoat dong (ACTIVE/INACTIVE), khong sua thong tin tai khoan
    UserResponse updateUserStatus(Long id, AdminUpdateUserRequest request);

    void deleteUser(Long id); // Soft delete
}