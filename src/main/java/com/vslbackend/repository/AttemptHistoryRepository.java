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
}
