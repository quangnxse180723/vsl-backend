package com.vslbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Mot luot thich cua 1 user cho 1 bai blog. Rang buoc unique(blog_id, user_id)
 * dam bao moi user chi thich 1 lan (toggle).
 */
@Entity
@Table(name = "blog_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"blog_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
