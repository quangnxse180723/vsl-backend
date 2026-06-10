package com.vslbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vocabularies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 255)
    private String word;

    @Column(length = 255)
    private String description;

    @Column(name = "video_tutorial_url", length = 255)
    private String videoTutorialUrl;
}
