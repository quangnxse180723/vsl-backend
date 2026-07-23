package com.vslbackend.service.impl;

import com.vslbackend.dto.response.BlogNotificationResponse;
import com.vslbackend.entity.*;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.BlogNotificationRepository;
import com.vslbackend.service.inter.BlogNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogNotificationServiceImpl implements BlogNotificationService {

    private final BlogNotificationRepository blogNotificationRepository;

    @Override
    public void notifyMention(User actor, User recipient, Blog blog, BlogComment comment, CommentReply reply) {
        if (shouldSkip(actor, recipient)) {
            return;
        }

        blogNotificationRepository.save(BlogNotification.builder()
                .recipient(recipient)
                .actor(actor)
                .blog(blog)
                .comment(comment)
                .reply(reply)
                .type(BlogNotificationType.MENTION)
                .message(displayName(actor) + " da nhac den ban trong binh luan")
                .build());
    }

    @Override
    public void notifyShare(User actor, User recipient, Blog blog) {
        if (shouldSkip(actor, recipient)) {
            return;
        }

        blogNotificationRepository.save(BlogNotification.builder()
                .recipient(recipient)
                .actor(actor)
                .blog(blog)
                .type(BlogNotificationType.SHARE)
                .message(displayName(actor) + " da chia se mot bai viet voi ban")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogNotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return blogNotificationRepository
                .findByRecipient_UserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    public BlogNotificationResponse markRead(Long notificationId, Long userId) {
        BlogNotification notification = blogNotificationRepository
                .findByIdAndRecipient_UserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.setRead(true);
        return toResponse(blogNotificationRepository.save(notification));
    }

    @Override
    public void markAllRead(Long userId) {
        blogNotificationRepository.markAllRead(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return blogNotificationRepository.countByRecipient_UserIdAndReadFalse(userId);
    }

    private boolean shouldSkip(User actor, User recipient) {
        return actor == null
                || recipient == null
                || actor.getUserId() == null
                || actor.getUserId().equals(recipient.getUserId());
    }

    private BlogNotificationResponse toResponse(BlogNotification notification) {
        User actor = notification.getActor();
        User recipient = notification.getRecipient();
        Blog blog = notification.getBlog();

        return BlogNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .read(notification.isRead())
                .message(notification.getMessage())
                .actorId(actor != null ? actor.getUserId() : null)
                .actorName(actor != null ? displayName(actor) : null)
                .actorAvatar(actor != null ? actor.getAvatarUrl() : null)
                .recipientId(recipient != null ? recipient.getUserId() : null)
                .blogId(blog != null ? blog.getId() : null)
                .blogTitle(blog != null ? blog.getTitle() : null)
                .commentId(notification.getComment() != null ? notification.getComment().getId() : null)
                .replyId(notification.getReply() != null ? notification.getReply().getId() : null)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }
}
