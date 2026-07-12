package com.vslbackend.repository;

import com.vslbackend.entity.BlogComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {

    @Query("SELECT c FROM BlogComment c JOIN FETCH c.user WHERE c.blog.id = :blogId ORDER BY c.createdAt DESC")
    List<BlogComment> findByBlogIdWithUser(@Param("blogId") Long blogId);

    long countByBlog_Id(Long blogId);

    void deleteByBlog_Id(Long blogId);
}
