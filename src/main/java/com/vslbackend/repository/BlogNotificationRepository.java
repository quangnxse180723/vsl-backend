package com.vslbackend.repository;

import com.vslbackend.entity.BlogNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogNotificationRepository extends JpaRepository<BlogNotification, Long> {
    Page<BlogNotification> findByRecipient_UserIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Optional<BlogNotification> findByIdAndRecipient_UserId(Long id, Long recipientId);
    long countByRecipient_UserIdAndReadFalse(Long recipientId);
    void deleteByBlog_Id(Long blogId);
    void deleteByComment_Id(Long commentId);
    void deleteByComment_Blog_Id(Long blogId);
    void deleteByReply_Id(Long replyId);
    void deleteByReply_Comment_Id(Long commentId);
    void deleteByReply_Comment_Blog_Id(Long blogId);

    @Modifying
    @Query("UPDATE BlogNotification n SET n.read = true WHERE n.recipient.userId = :recipientId")
    void markAllRead(@Param("recipientId") Long recipientId);
}
