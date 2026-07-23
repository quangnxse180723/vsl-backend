package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReplyResponse {
    private Long id;
    private Long commentId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long mentionedUserId;
    private String mentionedUserName;
    private String content;
    private LocalDateTime createdAt;
}
