package com.vslbackend.repository;

import com.vslbackend.entity.Role;
import com.vslbackend.entity.User;
import com.vslbackend.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByRole(Role role);

    @Query("""
        SELECT u FROM User u
        WHERE u.status = :status
          AND (
            LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY u.fullName ASC, u.username ASC
    """)
    Page<User> searchByNameOrUsername(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable);

    /**
     * Tim tat ca user du dieu kien can gui email nhac nho streak:
     * 1. Da bat thong bao email (emailNotificationsEnabled = true)
     * 2. Co streak (luyen tap it nhat 1 lan truoc do - co row trong attempt_history)
     * 3. CHUA luyen tap hom nay (khong co attempt nao trong ngay :today)
     * 4. CHUA bi gui mail nhac nho hom nay (streakReminderSentDate != :today)
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE u.emailNotificationsEnabled = true
          AND u.status = com.vslbackend.entity.UserStatus.ACTIVE
          AND (u.streakReminderSentDate IS NULL OR u.streakReminderSentDate <> :today)
          AND EXISTS (
              SELECT 1 FROM AttemptHistory a
              WHERE a.user = u
                AND CAST(a.attemptedAt AS date) = :today
          )
    """)
    List<User> findUsersNeedingStreakReminder(
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday
    );
}
