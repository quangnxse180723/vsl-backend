package com.vslbackend.repository;

import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogReport;
import com.vslbackend.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /**
     * Bai co don to cao NAO DANG CHO XU LY khong - dung de chan tac gia xoa bai khi don
     * to cao chua duoc quan tri vien giai quyet (xoa bai se cuon theo ca don to cao,
     * lam mat can cu chan dang lai noi dung do).
     */
    boolean existsByBlog_IdAndStatus(Long blogId, ReportStatus status);

    List<BlogReport> findByBlog_Id(Long blogId);

    /**
     * Cac bai DA TUNG BI TO CAO (moi bai 1 lan du co nhieu don). Dung de chan dang lai
     * (repost) noi dung da bi to cao.
     * <p>
     * So khop phai lam o Java qua {@link com.vslbackend.util.VietnameseText#fold} vi Postgres
     * khong bo dau tieng Viet duoc neu khong cai extension unaccent - lam o DB bang
     * LOWER()/TRIM() se bo lot cac bien the kieu "Sợ Hãi" -> "so hai". Danh sach nay chi gom
     * bai co don to cao nen rat nho.
     */
    @Query("SELECT DISTINCT r.blog FROM BlogReport r")
    List<Blog> findReportedBlogs();
}
