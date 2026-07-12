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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        int proficiency = AchievementServiceImpl.computeProficiency(learnedCount, totalVocabs);

        List<LocalDate> practiceDates = attemptHistoryRepository.findDistinctPracticeDates(userId);
        StreakResult streak = computeStreak(practiceDates, LocalDate.now());

        PracticeStatsResponse stats = PracticeStatsResponse.builder()
                .totalAttempts(totalAttempts)
                .correctAttempts(correctAttempts)
                .learnedCount(learnedCount)
                .totalVocabs(totalVocabs)
                .accuracyRate(accuracyRate)
                .proficiency(proficiency)
                .currentStreak(streak.current())
                .longestStreak(streak.longest())
                .weekActivity(streak.week())
                .build();

        return ResponseEntity.ok(ApiResponse.of("Lấy thống kê thành công", stats));
    }

    /** Ket qua tinh chuoi ngay hoc. */
    record StreakResult(int current, int longest, List<Boolean> week) {}

    /**
     * Tinh chuoi ngay hoc tu danh sach cac ngay co luyen tap (da sap xep moi -> cu).
     * - current: so ngay lien tiep tinh den hom nay. Chuoi con "song" khi ngay gan
     *   nhat la hom nay HOAC hom qua (chua het ngay hom nay); neu bo lo tu 2 ngay tro
     *   len thi current = 0 (dut chuoi, dem lai tu dau).
     * - longest: chuoi dai nhat trong toan bo lich su.
     * - week: 7 ngay gan nhat (index 0 = 6 ngay truoc ... index 6 = hom nay), true = co hoc.
     */
    static StreakResult computeStreak(List<LocalDate> datesDesc, LocalDate today) {
        Set<LocalDate> daySet = new HashSet<>(datesDesc);
        List<Boolean> week = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            week.add(daySet.contains(today.minusDays(i)));
        }

        if (datesDesc.isEmpty()) {
            return new StreakResult(0, 0, week);
        }

        // Chuoi hien tai: chi tinh neu ngay gan nhat la hom nay hoac hom qua.
        int current = 0;
        LocalDate mostRecent = datesDesc.get(0);
        if (mostRecent.equals(today) || mostRecent.equals(today.minusDays(1))) {
            current = 1;
            LocalDate prev = mostRecent;
            for (int i = 1; i < datesDesc.size(); i++) {
                LocalDate d = datesDesc.get(i);
                if (d.equals(prev.minusDays(1))) {
                    current++;
                    prev = d;
                } else {
                    break;
                }
            }
        }

        // Chuoi dai nhat: quet toan bo cac ngay lien tiep.
        int longest = 1;
        int run = 1;
        for (int i = 1; i < datesDesc.size(); i++) {
            if (datesDesc.get(i).equals(datesDesc.get(i - 1).minusDays(1))) {
                run++;
            } else {
                run = 1;
            }
            longest = Math.max(longest, run);
        }
        longest = Math.max(longest, current);

        return new StreakResult(current, longest, week);
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
