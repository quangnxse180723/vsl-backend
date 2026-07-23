package com.vslbackend.repository;

import com.vslbackend.entity.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long> {
    long countByReply_Id(Long replyId);
    boolean existsByReply_IdAndUser_UserId(Long replyId, Long userId);
    Optional<ReplyLike> findByReply_IdAndUser_UserId(Long replyId, Long userId);
    void deleteByReply_Id(Long replyId);
    void deleteByReply_Comment_Id(Long commentId);
    void deleteByReply_Comment_Blog_Id(Long blogId);
}
