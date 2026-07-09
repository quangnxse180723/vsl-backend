package com.vslbackend.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateBlogRequest {
    @NotBlank(message = "Tieu de bai viet khong duoc de trong")
    private String title;

    @NotBlank(message = "Noi dung bai viet khong duoc de trong")
    private String content;

    private String status = "DRAFT";
}
