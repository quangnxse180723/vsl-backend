package com.vslbackend.repository;

import com.vslbackend.entity.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    Optional<Vocabulary> findByExpectedId(Integer expectedId);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    Optional<Vocabulary> findById(Long id);

    @EntityGraph(attributePaths = {"category"})
    Page<Vocabulary> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Vocabulary> findByWordContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Vocabulary> findByCategoryId(Long categoryId, Pageable pageable);

}
