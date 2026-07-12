package com.vslbackend.repository;

import com.vslbackend.entity.BlogLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogLikeRepository extends JpaRepository<BlogLike, Long> {
    long countByBlog_Id(Long blogId);
    boolean existsByBlog_IdAndUser_UserId(Long blogId, Long userId);
    Optional<BlogLike> findByBlog_IdAndUser_UserId(Long blogId, Long userId);
    void deleteByBlog_Id(Long blogId);

    @Query("SELECT l FROM BlogLike l JOIN FETCH l.user WHERE l.blog.id = :blogId ORDER BY l.createdAt DESC")
    List<BlogLike> findByBlogIdWithUser(@Param("blogId") Long blogId);
}
