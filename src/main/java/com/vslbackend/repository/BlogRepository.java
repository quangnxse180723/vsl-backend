package com.vslbackend.repository;

import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Page<Blog> findByStatus(BlogStatus status, Pageable pageable);
    Page<Blog> findByAuthor_UserId(Long authorId, Pageable pageable);
}
