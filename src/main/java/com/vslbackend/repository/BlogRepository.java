package com.vslbackend.repository;

import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Page<Blog> findByStatus(BlogStatus status, Pageable pageable);
    Page<Blog> findByAuthor_UserId(Long authorId, Pageable pageable);

    /** Dem bai theo trang thai (vd chi dem PUBLISHED cho thong ke tong quan). */
    long countByStatus(BlogStatus status);

    /**
     * Danh sach blog cho admin: hien moi bai KHONG phai nhap (PUBLISHED + REMOVED)
     * cua tat ca moi nguoi, CONG voi bai nhap (DRAFT) cua chinh admin dang xem.
     * -> Admin khong thay bai nhap cua nguoi khac.
     */
    @Query("SELECT b FROM Blog b WHERE b.status <> com.vslbackend.entity.BlogStatus.DRAFT "
            + "OR b.author.userId = :adminId")
    Page<Blog> findAllForAdmin(@Param("adminId") Long adminId, Pageable pageable);
}
