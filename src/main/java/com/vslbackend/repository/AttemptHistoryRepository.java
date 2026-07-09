package com.vslbackend.repository;

import com.vslbackend.entity.AttemptHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptHistoryRepository extends JpaRepository<AttemptHistory, Long> {

    boolean existsByVocabulary_Id(Long vocabularyId);

    @Query(
            value = """
                    SELECT h FROM AttemptHistory h
                    LEFT JOIN FETCH h.vocabulary v
                    LEFT JOIN FETCH v.category
                    WHERE h.user.userId = :userId
                    ORDER BY h.attemptedAt DESC, h.id DESC
                    """,
            countQuery = "SELECT COUNT(h) FROM AttemptHistory h WHERE h.user.userId = :userId"
    )
    Page<AttemptHistory> findPageByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT h FROM AttemptHistory h
            LEFT JOIN FETCH h.vocabulary v
            LEFT JOIN FETCH v.category
            WHERE h.user.userId = :userId
            ORDER BY h.attemptedAt DESC, h.id DESC
            """)
    List<AttemptHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByUser_UserId(Long userId);

    long countByUser_UserIdAndIsCorrect(Long userId, boolean isCorrect);

    /**
     * Cac NGAY (khong tinh gio) ma nguoi dung co it nhat 1 lan luyen tap,
     * sap xep moi nhat truoc. Dung de tinh chuoi ngay hoc (streak).
     */
    @Query(value = "SELECT DISTINCT CAST(attempted_at AS date) AS d " +
                   "FROM attempt_history WHERE user_id = :userId ORDER BY d DESC",
           nativeQuery = true)
    List<java.time.LocalDate> findDistinctPracticeDates(@Param("userId") Long userId);
}
