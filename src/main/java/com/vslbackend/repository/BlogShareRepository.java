package com.vslbackend.repository;

import com.vslbackend.entity.BlogShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogShareRepository extends JpaRepository<BlogShare, Long> {

    long countByBlog_Id(Long blogId);

    void deleteByBlog_Id(Long blogId);

    // Lấy blog đã share lên profile của user
    Page<BlogShare> findByUser_UserIdAndShareType(
            Long userId, BlogShare.ShareType shareType, Pageable pageable);
}
