package com.vslbackend.repository;

import com.vslbackend.entity.BlogReport;
import com.vslbackend.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogReportRepository extends JpaRepository<BlogReport, Long> {

    @Query(value = "SELECT r FROM BlogReport r JOIN FETCH r.blog b JOIN FETCH r.reporter",
           countQuery = "SELECT COUNT(r) FROM BlogReport r")
    Page<BlogReport> findAllWithDetails(Pageable pageable);

    @Query(value = "SELECT r FROM BlogReport r JOIN FETCH r.blog b JOIN FETCH r.reporter WHERE r.status = :status",
           countQuery = "SELECT COUNT(r) FROM BlogReport r WHERE r.status = :status")
    Page<BlogReport> findByStatusWithDetails(@Param("status") ReportStatus status, Pageable pageable);

    void deleteByBlog_Id(Long blogId);

    long countByStatus(ReportStatus status);

    boolean existsByBlog_IdAndReporter_UserId(Long blogId, Long reporterId);

    java.util.List<BlogReport> findByBlog_Id(Long blogId);
}
