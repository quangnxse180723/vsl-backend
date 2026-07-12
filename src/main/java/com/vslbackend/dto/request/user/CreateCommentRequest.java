package com.vslbackend.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "Noi dung binh luan khong duoc de trong")
    private String content;
}
