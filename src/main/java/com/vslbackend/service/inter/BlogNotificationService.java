package com.vslbackend.service.inter;

import com.vslbackend.dto.response.BlogNotificationResponse;
import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogComment;
import com.vslbackend.entity.CommentReply;
import com.vslbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlogNotificationService {
    void notifyMention(User actor, User recipient, Blog blog, BlogComment comment, CommentReply reply);
    void notifyShare(User actor, User recipient, Blog blog);
    Page<BlogNotificationResponse> getNotifications(Long userId, Pageable pageable);
    BlogNotificationResponse markRead(Long notificationId, Long userId);
    void markAllRead(Long userId);
    long countUnread(Long userId);
}
