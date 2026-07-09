package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PracticeStatsResponse {
    private final long totalAttempts;
    private final long correctAttempts;
    private final long learnedCount;
    private final long totalVocabs;
    private final double accuracyRate;   // 0.0 – 100.0
    private final int proficiency;       // 0 – 100 (composite score)
    private final int currentStreak;     // so ngay lien tiep co luyen tap tinh den hom nay
    private final int longestStreak;     // chuoi ngay dai nhat tu truoc den nay
    private final List<Boolean> weekActivity; // 7 phan tu, cu -> moi; phan tu cuoi = hom nay
}
