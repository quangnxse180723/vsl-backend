package com.vslbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /** Mac dinh TAT - nguoi dung phai tu BAT de nhan email nhac nho streak. */
    @Column(name = "email_notifications_enabled", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean emailNotificationsEnabled = false;

    /**
     * Ngay da gui mail nhac nho streak lan cuoi.
     * Dung de dam bao chi gui DUNG 1 LAN moi ngay, du cron job chay nhieu lan.
     * Set ve null khi nguoi dung hoc xong (streak duoc tinh ngay hom nay).
     */
    @Column(name = "streak_reminder_sent_date")
    private LocalDate streakReminderSentDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
