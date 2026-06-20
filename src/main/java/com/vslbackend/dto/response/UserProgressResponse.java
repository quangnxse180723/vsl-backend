package com.vslbackend.dto.response;

import com.vslbackend.entity.LearningStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProgressResponse {
    private final Long vocabularyId;
    private final String word;
    private final String categoryName;
    private final LearningStatus learningStatus;
    private final LocalDateTime lastAttemptedAt;
}
