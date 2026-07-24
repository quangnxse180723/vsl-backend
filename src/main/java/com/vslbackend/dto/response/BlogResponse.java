package com.vslbackend.dto.response;

import com.vslbackend.entity.BlogStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BlogResponse {
    private Long id;
    private String title;
    private String content;
    private String thumbnailUrl;
    private BlogStatus status;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Tuong tac
    private long likeCount;
    private long commentCount;
    private boolean likedByMe;
    private boolean followedAuthor;
    private boolean friendWithAuthor;

    // Chi set khi status = REMOVED (bai bi admin go) - chi tac gia thay o "bai cua toi"
    private String deletionReason;
}
