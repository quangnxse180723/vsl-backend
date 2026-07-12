package com.vslbackend.repository;

import com.vslbackend.entity.VisitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    /** Danh sach log (kem user) moi nhat truoc - cho bang chi tiet phan trang. */
    @Query(value = "SELECT v FROM VisitLog v LEFT JOIN FETCH v.user ORDER BY v.visitedAt DESC",
            countQuery = "SELECT COUNT(v) FROM VisitLog v")
    Page<VisitLog> findAllWithUser(Pageable pageable);

    /** Tim luot truy cap theo sessionId de upsert (nang cap guest -> user). */
    Optional<VisitLog> findBySessionId(String sessionId);

    /** Chi lay moc thoi gian trong khoang - de gom nhom cho bieu do. */
    @Query("SELECT v.visitedAt FROM VisitLog v WHERE v.visitedAt BETWEEN :from AND :to")
    List<LocalDateTime> findVisitedAtBetween(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}
