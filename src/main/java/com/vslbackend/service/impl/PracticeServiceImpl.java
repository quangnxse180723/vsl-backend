package com.vslbackend.service.impl;

import com.vslbackend.dto.response.PracticeStatsResponse;
import com.vslbackend.dto.response.UserProgressResponse;
import com.vslbackend.entity.LearningStatus;
import com.vslbackend.entity.UserProgress;
import com.vslbackend.repository.AttemptHistoryRepository;
import com.vslbackend.repository.UserProgressRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.PracticeStreakCalculator;
import com.vslbackend.service.inter.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeServiceImpl implements PracticeService {

    private final AttemptHistoryRepository attemptHistoryRepository;
    private final UserProgressRepository userProgressRepository;
    private final VocabularyRepository vocabularyRepository;

    @Override
    public List<UserProgressResponse> getMyProgress(Long userId) {
        return userProgressRepository.findAllWithVocabularyByUserId(userId).stream()
                .map(this::toProgressResponse)
                .toList();
    }

    @Override
    public PracticeStatsResponse getStats(Long userId) {
        long totalAttempts = attemptHistoryRepository.countByUser_UserId(userId);
        long correctAttempts = attemptHistoryRepository.countByUser_UserIdAndIsCorrect(userId, true);
        long learnedCount = userProgressRepository.countByUser_UserIdAndLearningStatus(userId, LearningStatus.LEARNED);
        long totalVocabs = vocabularyRepository.count();

        double accuracyRate = totalAttempts > 0
                ? Math.round((double) correctAttempts / totalAttempts * 10000.0) / 100.0
                : 0.0;

        int proficiency = AchievementServiceImpl.computeProficiency(learnedCount, totalVocabs);
        List<LocalDate> practiceDates = attemptHistoryRepository.findDistinctPracticeDates(userId);
        PracticeStreakCalculator.StreakResult streak =
                PracticeStreakCalculator.computeStreak(practiceDates, LocalDate.now());

        return PracticeStatsResponse.builder()
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
    }

    private UserProgressResponse toProgressResponse(UserProgress progress) {
        return UserProgressResponse.builder()
                .vocabularyId(progress.getVocabulary().getId())
                .word(progress.getVocabulary().getWord())
                .categoryName(progress.getVocabulary().getCategory().getName())
                .learningStatus(progress.getLearningStatus())
                .lastAttemptedAt(progress.getLastAttemptedAt())
                .build();
    }
}
