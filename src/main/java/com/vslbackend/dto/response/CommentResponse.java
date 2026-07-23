package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long blogId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long mentionedUserId;
    private String mentionedUserName;
    private String content;
    private LocalDateTime createdAt;
    private List<ReplyResponse> replies;
    private long replyCount;
    private long likeCount;
    private boolean likedByMe;
}
