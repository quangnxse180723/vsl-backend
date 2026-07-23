package com.vslbackend.repository;

import com.vslbackend.entity.BlogShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogShareRepository extends JpaRepository<BlogShare, Long> {

    long countByBlog_Id(Long blogId);

    void deleteByBlog_Id(Long blogId);

    // Lấy blog đã share lên profile của user
    Page<BlogShare> findByUser_UserIdAndShareType(
            Long userId, BlogShare.ShareType shareType, Pageable pageable);

    Page<BlogShare> findByRecipientUser_UserIdAndShareType(
            Long recipientUserId, BlogShare.ShareType shareType, Pageable pageable);

    @Query("""
        SELECT s FROM BlogShare s
        LEFT JOIN s.recipientUser r
        WHERE s.shareType = :shareType
          AND (
            r.userId = :userId
            OR (r IS NULL AND s.user.userId = :userId)
          )
        ORDER BY s.createdAt DESC
    """)
    Page<BlogShare> findProfileSharesVisibleToUser(
            @Param("userId") Long userId,
            @Param("shareType") BlogShare.ShareType shareType,
            Pageable pageable);
}
