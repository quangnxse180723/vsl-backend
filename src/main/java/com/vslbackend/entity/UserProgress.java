package com.vslbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
// Rang buoc UNIQUE (user_id, vocabulary_id): moi user chi co DUNG 1 dong tien do
// cho 1 tu vung. Day la phong tuyen cap DB chong dem trung khi tinh % Chuong
// Trinh Hoc - ke ca neu co request ghi de dong thoi (race condition), DB se
// chan insert trung thay vi tao 2 dong cho cung 1 cap user+tu vung.
@Table(name = "user_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_progress_user_vocab", columnNames = {"user_id", "vocabulary_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    private Vocabulary vocabulary;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_status")
    private LearningStatus learningStatus;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;
}
