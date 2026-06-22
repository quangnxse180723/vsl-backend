package com.vslbackend.service.inter;

import com.vslbackend.dto.request.user.ChangePasswordRequest;
import com.vslbackend.dto.request.user.UpdateUserRequest;
import com.vslbackend.dto.response.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface UserService {

    UserResponse getCurrentUser();

    UserResponse updateProfile(UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);

    String uploadAvatar(MultipartFile file);

    UserResponse updateAvatar(String avatarUrl);

    void deleteCurrentUser();
}