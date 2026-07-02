package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PracticeStatsResponse {
    private final long totalAttempts;
    private final long correctAttempts;
    private final long learnedCount;
    private final long totalVocabs;
    private final double accuracyRate;   // 0.0 – 100.0
    private final int proficiency;       // 0 – 100 (composite score)
}
