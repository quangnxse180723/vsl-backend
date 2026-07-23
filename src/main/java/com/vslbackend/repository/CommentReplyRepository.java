package com.vslbackend.repository;

import com.vslbackend.entity.CommentReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReplyRepository extends JpaRepository<CommentReply, Long> {

    @Query("SELECT r FROM CommentReply r " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.mentionedUser " +
            "WHERE r.comment.id = :commentId " +
            "ORDER BY r.createdAt ASC")
    Page<CommentReply> findByCommentIdWithUsers(@Param("commentId") Long commentId,
                                                Pageable pageable);

    void deleteByComment_Id(Long commentId);

    long countByComment_Id(Long commentId);
}
