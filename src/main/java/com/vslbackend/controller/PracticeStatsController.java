package com.vslbackend.controller;

import com.vslbackend.dto.response.AchievementResponse;
import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.PracticeStatsResponse;
import com.vslbackend.entity.LearningStatus;
import com.vslbackend.repository.AttemptHistoryRepository;
import com.vslbackend.repository.UserProgressRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.impl.AchievementServiceImpl;
import com.vslbackend.service.inter.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeStatsController {

    private final AttemptHistoryRepository  attemptHistoryRepository;
    private final UserProgressRepository    userProgressRepository;
    private final VocabularyRepository      vocabularyRepository;
    private final AchievementService        achievementService;

    /**
     * GET /api/practice/stats
     * Trả về thống kê thực hành + điểm thành thạo của người dùng hiện tại.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PracticeStatsResponse>> getStats(
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getUser().getUserId();

        long totalAttempts   = attemptHistoryRepository.countByUser_UserId(userId);
        long correctAttempts = attemptHistoryRepository.countByUser_UserIdAndIsCorrect(userId, true);
        long learnedCount    = userProgressRepository.countByUser_UserIdAndLearningStatus(userId, LearningStatus.LEARNED);
        long totalVocabs     = vocabularyRepository.count();

        double accuracyRate = totalAttempts > 0
                ? Math.round((double) correctAttempts / totalAttempts * 10000.0) / 100.0
                : 0.0;

        int proficiency = AchievementServiceImpl.computeProficiency(
                learnedCount, totalVocabs, correctAttempts, totalAttempts);

        PracticeStatsResponse stats = PracticeStatsResponse.builder()
                .totalAttempts(totalAttempts)
                .correctAttempts(correctAttempts)
                .learnedCount(learnedCount)
                .totalVocabs(totalVocabs)
                .accuracyRate(accuracyRate)
                .proficiency(proficiency)
                .build();

        return ResponseEntity.ok(ApiResponse.of("Lấy thống kê thành công", stats));
    }

    /**
     * GET /api/achievements
     * Danh sách tất cả thành tựu kèm trạng thái mở khóa của người dùng.
     */
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<List<AchievementResponse>>> getAchievements(
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getUser().getUserId();
        List<AchievementResponse> achievements = achievementService.getAll(userId);
        return ResponseEntity.ok(ApiResponse.of("Lấy thành tựu thành công", achievements));
    }
}
