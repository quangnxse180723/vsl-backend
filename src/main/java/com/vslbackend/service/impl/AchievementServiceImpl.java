package com.vslbackend.service.impl;

import com.vslbackend.dto.response.AchievementResponse;
import com.vslbackend.entity.Achievement;
import com.vslbackend.entity.LearningStatus;
import com.vslbackend.entity.User;
import com.vslbackend.entity.UserAchievement;
import com.vslbackend.repository.*;
import com.vslbackend.service.inter.AchievementService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository        achievementRepository;
    private final UserAchievementRepository    userAchievementRepository;
    private final AttemptHistoryRepository     attemptHistoryRepository;
    private final UserProgressRepository       userProgressRepository;
    private final VocabularyRepository         vocabularyRepository;
    private final UserRepository               userRepository;

    // ──────────────────────────────────────────────────────────────
    //  Seed achievements once on startup (idempotent)
    // ──────────────────────────────────────────────────────────────

    record AchievementDef(Long id, String key, String name, String description, String iconKey) {}

    private static final List<AchievementDef> DEFINITIONS = List.of(
        new AchievementDef(1L, "FIRST_STEP",      "Bước Đầu",       "Hoàn thành lần thực hành đầu tiên",       "directions_walk"),
        new AchievementDef(2L, "CORRECT_10",       "Chính Xác",      "Đạt 10 lần đánh giá đúng",                "check_circle"),
        new AchievementDef(3L, "CORRECT_50",       "Thành Thạo",     "Đạt 50 lần đánh giá đúng",                "military_tech"),
        new AchievementDef(4L, "LEARNED_5",        "Nhập Môn",       "Học thuộc 5 từ vựng",                     "school"),
        new AchievementDef(5L, "LEARNED_20",       "Tiến Bộ",        "Học thuộc 20 từ vựng",                    "trending_up"),
        new AchievementDef(6L, "ALL_LEARNED",      "Đại Sư",         "Học thuộc tất cả từ vựng trong hệ thống", "emoji_events"),
        new AchievementDef(7L, "PROFICIENCY_50",   "Nửa Đường",      "Đạt độ thành thạo 50%",                   "star_half"),
        new AchievementDef(8L, "PROFICIENCY_100",  "Hoàn Hảo",       "Đạt độ thành thạo 100%",                  "stars")
    );

    @PostConstruct
    @Transactional
    public void seedAchievements() {
        for (AchievementDef def : DEFINITIONS) {
            if (!achievementRepository.existsByKey(def.key())) {
                achievementRepository.save(Achievement.builder()
                        .id(def.id())
                        .key(def.key())
                        .name(def.name())
                        .description(def.description())
                        .iconKey(def.iconKey())
                        .build());
            }
        }
        log.info("Achievements seeded ({} definitions).", DEFINITIONS.size());
    }

    // ──────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAll(Long userId) {
        List<Achievement> all = achievementRepository.findAll();
        Set<String> unlocked = userAchievementRepository.findUnlockedKeysByUserId(userId);

        // Also fetch unlock timestamps for unlocked ones
        Map<String, LocalDateTime> unlockedAt = new HashMap<>();
        userAchievementRepository.findAll().stream()
                .filter(ua -> ua.getUser().getUserId().equals(userId))
                .forEach(ua -> unlockedAt.put(ua.getAchievement().getKey(), ua.getUnlockedAt()));

        return all.stream()
                .sorted(Comparator.comparingLong(Achievement::getId))
                .map(a -> AchievementResponse.builder()
                        .id(a.getId())
                        .key(a.getKey())
                        .name(a.getName())
                        .description(a.getDescription())
                        .iconKey(a.getIconKey())
                        .unlocked(unlocked.contains(a.getKey()))
                        .unlockedAt(unlockedAt.get(a.getKey()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void checkAndUnlock(Long userId) {
        Set<String> alreadyUnlocked = userAchievementRepository.findUnlockedKeysByUserId(userId);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        long totalAttempts  = attemptHistoryRepository.countByUser_UserId(userId);
        long correctAttempts = attemptHistoryRepository.countByUser_UserIdAndIsCorrect(userId, true);
        long learnedCount   = userProgressRepository.countByUser_UserIdAndLearningStatus(userId, LearningStatus.LEARNED);
        long totalVocabs    = vocabularyRepository.count();
        int  proficiency    = computeProficiency(learnedCount, totalVocabs, correctAttempts, totalAttempts);

        Map<String, Boolean> conditions = new LinkedHashMap<>();
        conditions.put("FIRST_STEP",     totalAttempts  >= 1);
        conditions.put("CORRECT_10",     correctAttempts >= 10);
        conditions.put("CORRECT_50",     correctAttempts >= 50);
        conditions.put("LEARNED_5",      learnedCount   >= 5);
        conditions.put("LEARNED_20",     learnedCount   >= 20);
        conditions.put("ALL_LEARNED",    totalVocabs > 0 && learnedCount >= totalVocabs);
        conditions.put("PROFICIENCY_50", proficiency    >= 50);
        conditions.put("PROFICIENCY_100",proficiency    >= 100);

        LocalDateTime now = LocalDateTime.now();
        conditions.forEach((key, met) -> {
            if (met && !alreadyUnlocked.contains(key)) {
                achievementRepository.findByKey(key).ifPresent(achievement -> {
                    userAchievementRepository.save(UserAchievement.builder()
                            .user(user)
                            .achievement(achievement)
                            .unlockedAt(now)
                            .build());
                    log.info("Achievement unlocked: userId={} key={}", userId, key);
                });
            }
        });
    }

    // ──────────────────────────────────────────────────────────────
    //  Proficiency formula (also used by StatsService)
    //  60% weight = learned rate, 40% weight = accuracy rate
    // ──────────────────────────────────────────────────────────────
    public static int computeProficiency(long learned, long totalVocabs,
                                         long correct, long totalAttempts) {
        double learnRate    = totalVocabs    > 0 ? (double) learned  / totalVocabs    : 0.0;
        double accuracyRate = totalAttempts  > 0 ? (double) correct  / totalAttempts  : 0.0;
        return (int) Math.round(learnRate * 60 + accuracyRate * 40);
    }
}
