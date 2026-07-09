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
}
