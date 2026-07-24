package com.vslbackend.repository;

import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Page<Blog> findByStatus(BlogStatus status, Pageable pageable);
    Page<Blog> findByAuthor_UserId(Long authorId, Pageable pageable);
    Page<Blog> findByStatusAndAuthor_UserId(BlogStatus status, Long authorId, Pageable pageable);

    /** Dem bai theo trang thai (vd chi dem PUBLISHED cho thong ke tong quan). */
    long countByStatus(BlogStatus status);

    /**
     * Tat ca bai da cong khai (PUBLISHED) - dung de chan dang trung noi dung.
     * So khop phai lam o Java qua {@link com.vslbackend.util.VietnameseText#fold} vi Postgres
     * khong bo dau tieng Viet duoc neu khong cai extension unaccent (giong findReportedBlogs).
     */
    @Query("SELECT b FROM Blog b WHERE b.status = com.vslbackend.entity.BlogStatus.PUBLISHED")
    List<Blog> findAllPublished();

    /**
     * Danh sach blog cho admin: hien moi bai KHONG phai nhap (PUBLISHED + REMOVED)
     * cua tat ca moi nguoi, CONG voi bai nhap (DRAFT) cua chinh admin dang xem.
     * -> Admin khong thay bai nhap cua nguoi khac.
     */
    @Query("SELECT b FROM Blog b WHERE b.status <> com.vslbackend.entity.BlogStatus.DRAFT "
            + "OR b.author.userId = :adminId")
    Page<Blog> findAllForAdmin(@Param("adminId") Long adminId, Pageable pageable);

    @Query(value = """
        SELECT b FROM Blog b
        LEFT JOIN UserFollow f
          ON f.following = b.author
         AND f.follower.userId = :currentUserId
        WHERE b.status = com.vslbackend.entity.BlogStatus.PUBLISHED
        ORDER BY
            CASE WHEN f.id IS NULL THEN 1 ELSE 0 END,
            b.createdAt DESC
    """, countQuery = """
        SELECT COUNT(b) FROM Blog b
        WHERE b.status = com.vslbackend.entity.BlogStatus.PUBLISHED
    """)
    Page<Blog> findPublishedPrioritizingFollowed(
            @Param("currentUserId") Long currentUserId,
            Pageable pageable);

    @Query(value = """
        SELECT b FROM Blog b
        LEFT JOIN b.author a
        WHERE b.status = com.vslbackend.entity.BlogStatus.PUBLISHED
          AND (
            LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(a.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(a.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY b.createdAt DESC
    """, countQuery = """
        SELECT COUNT(b) FROM Blog b
        LEFT JOIN b.author a
        WHERE b.status = com.vslbackend.entity.BlogStatus.PUBLISHED
          AND (
            LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(a.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(a.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Blog> searchPublished(@Param("keyword") String keyword, Pageable pageable);
}
