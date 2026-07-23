package com.vslbackend.repository;

import com.vslbackend.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    long countByComment_Id(Long commentId);
    boolean existsByComment_IdAndUser_UserId(Long commentId, Long userId);
    Optional<CommentLike> findByComment_IdAndUser_UserId(Long commentId, Long userId);
    void deleteByComment_Id(Long commentId);
    void deleteByComment_Blog_Id(Long blogId);
}
