package com.vslbackend.dto.response;

import com.vslbackend.entity.BlogNotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BlogNotificationResponse {
    private Long id;
    private BlogNotificationType type;
    private boolean read;
    private String message;
    private Long actorId;
    private String actorName;
    private String actorAvatar;
    private Long recipientId;
    private Long blogId;
    private String blogTitle;
    private Long commentId;
    private Long replyId;
    private LocalDateTime createdAt;
}
