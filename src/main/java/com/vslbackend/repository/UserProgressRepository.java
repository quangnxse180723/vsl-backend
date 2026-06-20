package com.vslbackend.repository;

import com.vslbackend.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUser_UserIdAndVocabulary_Id(Long userId, Long vocabularyId);

    @Query("SELECT p FROM UserProgress p " +
           "JOIN FETCH p.vocabulary v " +
           "JOIN FETCH v.category " +
           "WHERE p.user.userId = :userId " +
           "ORDER BY p.lastAttemptedAt DESC")
    List<UserProgress> findAllWithVocabularyByUserId(@Param("userId") Long userId);
}
