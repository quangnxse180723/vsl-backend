package com.vslbackend.scheduler;

import com.vslbackend.entity.User;
import com.vslbackend.repository.AttemptHistoryRepository;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.service.PracticeStreakCalculator;
import com.vslbackend.service.inter.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Cron job chay moi gio, phat hien cac user sap bi mat streak va gui email nhac nho.
 *
 * Logic:
 *   - Chay vao phut thu 0 cua moi gio (0 * * * * ?)
 *   - Gio nhay vao gui mail: khi dong ho chi 19h (con 5 gio truoc nua dem reset)
 *   - Neu user van chua hoc sau khi da nhan mail, khong gui them (streakReminderSentDate da duoc dat)
 *   - Sau khi user hoc xong, streakReminderSentDate se duoc reset ve null boi AttemptService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakReminderScheduler {

    /** Gio (theo gio VN = UTC+7) ma cron job se gui mail. 19h = con 5 tieng truoc 0h. */
    private static final int REMINDER_HOUR_VN = 19;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository         userRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final EmailService           emailService;

    /**
     * CHAY DEMO: Chay moi phut
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendStreakReminders() {
        ZonedDateTime nowVn = ZonedDateTime.now(VIETNAM_ZONE);
        int currentHour = nowVn.getHour();

        LocalDate today = nowVn.toLocalDate();
        LocalDate yesterday = today.minusDays(1);

        log.info("[StreakReminder DEMO] Bat dau quet user can gui nhac nho streak ({}h VN, ngay {})",
                currentHour, today);

        // Lay danh sach user thoa man dieu kien: bat thong bao + co streak + chua hoc hom nay + chua duoc gui mail hom nay
        List<User> usersToRemind = userRepository.findUsersNeedingStreakReminder(today, yesterday);

        if (usersToRemind.isEmpty()) {
            log.info("[StreakReminder] Khong co user nao can gui nhac nho hom nay.");
            return;
        }

        log.info("[StreakReminder] Tim thay {} user can gui nhac nho.", usersToRemind.size());

        for (User user : usersToRemind) {
            try {
                // Tinh streak hien tai cua user nay
                List<LocalDate> practiceDates = attemptHistoryRepository.findDistinctPracticeDates(user.getUserId());
                PracticeStreakCalculator.StreakResult streak =
                        PracticeStreakCalculator.computeStreak(practiceDates, today);

                int currentStreak = streak.current();

                // Khong gui neu streak = 0 (chua bao gio hoc, hoac da bi mat chuoi)
                if (currentStreak <= 0) {
                    log.debug("[StreakReminder] User {} co streak = 0, bo qua.", user.getEmail());
                    continue;
                }

                // Gui email nhac nho bat dong bo
                emailService.sendStreakReminderEmail(
                        user.getEmail(),
                        user.getFullName(),
                        currentStreak
                );

                // Danh dau da gui mail ngay hom nay de tranh gui trung
                user.setStreakReminderSentDate(today);
                userRepository.save(user);

                log.info("[StreakReminder] Da gui nhac nho cho user {} (streak={}).",
                        user.getEmail(), currentStreak);

            } catch (Exception e) {
                // Loi cua 1 user khong duoc lam hong job cua cac user con lai
                log.error("[StreakReminder] Loi khi xu ly user {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("[StreakReminder] Hoan thanh. Da xu ly {} user.", usersToRemind.size());
    }
}
