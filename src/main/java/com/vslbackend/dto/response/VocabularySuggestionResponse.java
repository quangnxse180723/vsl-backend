package com.vslbackend.dto.response;

import com.vslbackend.entity.SuggestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VocabularySuggestionResponse {
    private Long id;
    private String word;
    private String description;
    private SuggestionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    // Danh muc de xuat
    private Long categoryId;
    private String categoryName;

    // Nguoi gui de xuat
    private Long requesterId;
    private String requesterName;
}
