package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long blogId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
}
