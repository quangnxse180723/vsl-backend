package com.vslbackend.dto.request.admin;

import com.vslbackend.entity.UserStatus;
import lombok.Data;

/** Admin chi duoc phep bat/tat trang thai hoat dong cua tai khoan. */
@Data
public class AdminUpdateUserRequest {
    private UserStatus status;
}
