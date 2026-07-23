package com.vslbackend.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReplyRequest {
    @NotBlank(message = "Noi dung tra loi khong duoc de trong")
    private String content;

    private Long mentionedUserId;
}
