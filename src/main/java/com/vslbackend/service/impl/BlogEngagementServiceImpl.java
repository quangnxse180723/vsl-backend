package com.vslbackend.service.impl;

import com.vslbackend.dto.response.*;
import com.vslbackend.entity.*;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.*;
import com.vslbackend.service.inter.BlogEngagementService;
import com.vslbackend.service.inter.BlogNotificationService;
import com.vslbackend.service.inter.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogEngagementServiceImpl implements BlogEngagementService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final BlogReportRepository blogReportRepository;
    private final CommentReplyRepository commentReplyRepository;
    private final BlogShareRepository blogShareRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final BlogNotificationRepository blogNotificationRepository;
    private final BlogNotificationService blogNotificationService;
    private final FollowService followService;

    private Blog getBlogOrThrow(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public LikeResponse toggleLike(Long blogId, Long userId) {
        Blog blog = getBlogOrThrow(blogId);

        boolean nowLiked;
        if (blogLikeRepository.existsByBlog_IdAndUser_UserId(blogId, userId)) {
            blogLikeRepository.findByBlog_IdAndUser_UserId(blogId, userId)
                    .ifPresent(blogLikeRepository::delete);
            nowLiked = false;
        } else {
            User user = getUserOrThrow(userId);
            blogLikeRepository.save(BlogLike.builder()
                    .blog(blog)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build());
            nowLiked = true;
        }

        return LikeResponse.builder()
                .liked(nowLiked)
                .likeCount(blogLikeRepository.countByBlog_Id(blogId))
                .build();
    }

    @Override
    public LikeResponse toggleCommentLike(Long commentId, Long userId) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        boolean nowLiked;
        if (commentLikeRepository.existsByComment_IdAndUser_UserId(commentId, userId)) {
            commentLikeRepository.findByComment_IdAndUser_UserId(commentId, userId)
                    .ifPresent(commentLikeRepository::delete);
            nowLiked = false;
        } else {
            User user = getUserOrThrow(userId);
            commentLikeRepository.save(CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build());
            nowLiked = true;
        }

        return LikeResponse.builder()
                .liked(nowLiked)
                .likeCount(commentLikeRepository.countByComment_Id(commentId))
                .build();
    }

    @Override
    public LikeResponse toggleReplyLike(Long replyId, Long userId) {
        CommentReply reply = commentReplyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        boolean nowLiked;
        if (replyLikeRepository.existsByReply_IdAndUser_UserId(replyId, userId)) {
            replyLikeRepository.findByReply_IdAndUser_UserId(replyId, userId)
                    .ifPresent(replyLikeRepository::delete);
            nowLiked = false;
        } else {
            User user = getUserOrThrow(userId);
            replyLikeRepository.save(ReplyLike.builder()
                    .reply(reply)
                    .user(user)
                    .build());
            nowLiked = true;
        }

        return LikeResponse.builder()
                .liked(nowLiked)
                .likeCount(replyLikeRepository.countByReply_Id(replyId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long blogId, Long currentUserId) {
        getBlogOrThrow(blogId);
        return blogCommentRepository.findByBlogIdWithUser(blogId).stream()
                .map(comment -> toCommentResponse(comment, currentUserId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LikeUserResponse> getLikers(Long blogId) {
        return blogLikeRepository.findByBlogIdWithUser(blogId).stream()
                .map(this::toLikeUserResponse)
                .toList();
    }

    @Override
    public CommentResponse addComment(Long blogId, Long userId, Long mentionedUserId, String content) {
        Blog blog = getBlogOrThrow(blogId);
        User user = getUserOrThrow(userId);
        User mentionedUser = mentionedUserId != null ? getUserOrThrow(mentionedUserId) : null;

        BlogComment comment = blogCommentRepository.save(BlogComment.builder()
                .blog(blog)
                .user(user)
                .mentionedUser(mentionedUser)
                .content(requireContent(content))
                .build());

        blogNotificationService.notifyMention(user, mentionedUser, blog, comment, null);
        return toCommentResponse(comment, userId);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.getUser() == null || !comment.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen xoa binh luan nay");
        }

        blogNotificationRepository.deleteByReply_Comment_Id(commentId);
        replyLikeRepository.deleteByReply_Comment_Id(commentId);
        commentReplyRepository.deleteByComment_Id(commentId);
        blogNotificationRepository.deleteByComment_Id(commentId);
        commentLikeRepository.deleteByComment_Id(commentId);
        blogCommentRepository.delete(comment);
    }

    @Override
    public void reportBlog(Long blogId, Long userId, String reason) {
        Blog blog = getBlogOrThrow(blogId);
        if (blog.getAuthor() != null && blog.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_REPORT_OWN_BLOG);
        }
        if (blogReportRepository.existsByBlog_IdAndReporter_UserId(blogId, userId)) {
            throw new AppException(ErrorCode.ALREADY_REPORTED);
        }
        User reporter = getUserOrThrow(userId);
        blogReportRepository.save(BlogReport.builder()
                .blog(blog)
                .reporter(reporter)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .build());
    }

    @Override
    public ReplyResponse addReply(Long commentId, Long userId, Long mentionedUserId, String content) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        User user = getUserOrThrow(userId);
        User mentionedUser = mentionedUserId != null ? getUserOrThrow(mentionedUserId) : null;

        CommentReply reply = commentReplyRepository.save(CommentReply.builder()
                .comment(comment)
                .user(user)
                .mentionedUser(mentionedUser)
                .content(requireContent(content))
                .build());

        blogNotificationService.notifyMention(user, mentionedUser, comment.getBlog(), comment, reply);
        return toReplyResponse(reply, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReplyResponse> getReplies(Long commentId, Long currentUserId, Pageable pageable) {
        if (!blogCommentRepository.existsById(commentId)) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return commentReplyRepository
                .findByCommentIdWithUsers(commentId, pageable)
                .map(reply -> toReplyResponse(reply, currentUserId));
    }

    @Override
    public void deleteReply(Long replyId, Long userId) {
        CommentReply reply = commentReplyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!reply.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen xoa reply nay");
        }

        blogNotificationRepository.deleteByReply_Id(replyId);
        replyLikeRepository.deleteByReply_Id(replyId);
        commentReplyRepository.delete(reply);
    }

    @Override
    public ShareResponse shareBlog(Long blogId, Long userId, String shareType, Long recipientUserId) {
        Blog blog = getBlogOrThrow(blogId);
        User user = getUserOrThrow(userId);
        BlogShare.ShareType type = parseShareType(shareType);

        User recipient = null;
        if (type == BlogShare.ShareType.PROFILE && recipientUserId != null) {
            if (recipientUserId.equals(userId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Khong the chia se qua profile cho chinh minh");
            }
            if (!followService.areFriends(userId, recipientUserId)) {
                throw new AppException(ErrorCode.NOT_FRIEND);
            }
            recipient = getUserOrThrow(recipientUserId);
        }

        blogShareRepository.save(BlogShare.builder()
                .blog(blog)
                .user(user)
                .recipientUser(recipient)
                .shareType(type)
                .build());

        if (recipient != null) {
            blogNotificationService.notifyShare(user, recipient, blog);
        }

        String blogUrl = "https://www.sighmentor.click";

        return ShareResponse.builder()
                .shareCount(blogShareRepository.countByBlog_Id(blogId))
                .blogUrl(blogUrl)
                .shareType(type.name())
                .recipientUserId(recipient != null ? recipient.getUserId() : null)
                .recipientName(recipient != null ? displayName(recipient) : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blogReportRepository.findAllWithDetails(pageable).map(this::toReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingReports() {
        return blogReportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    public void resolveReport(Long reportId) {
        BlogReport report = blogReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        blogReportRepository.save(report);
    }

    private LikeUserResponse toLikeUserResponse(BlogLike like) {
        User user = like.getUser();
        return LikeUserResponse.builder()
                .userId(user != null ? user.getUserId() : null)
                .userName(user != null ? displayName(user) : null)
                .userAvatar(user != null ? user.getAvatarUrl() : null)
                .createdAt(like.getCreatedAt())
                .build();
    }

    private CommentResponse toCommentResponse(BlogComment comment, Long currentUserId) {
        User user = comment.getUser();
        User mentioned = comment.getMentionedUser();
        Long commentId = comment.getId();

        return CommentResponse.builder()
                .id(commentId)
                .blogId(comment.getBlog() != null ? comment.getBlog().getId() : null)
                .userId(user != null ? user.getUserId() : null)
                .userName(user != null ? displayName(user) : null)
                .userAvatar(user != null ? user.getAvatarUrl() : null)
                .mentionedUserId(mentioned != null ? mentioned.getUserId() : null)
                .mentionedUserName(mentioned != null ? displayName(mentioned) : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .replyCount(commentReplyRepository.countByComment_Id(commentId))
                .likeCount(commentLikeRepository.countByComment_Id(commentId))
                .likedByMe(currentUserId != null
                        && commentLikeRepository.existsByComment_IdAndUser_UserId(commentId, currentUserId))
                .build();
    }

    private ReportResponse toReportResponse(BlogReport report) {
        Blog blog = report.getBlog();
        User reporter = report.getReporter();
        return ReportResponse.builder()
                .id(report.getId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .reporterId(reporter != null ? reporter.getUserId() : null)
                .reporterName(reporter != null ? displayName(reporter) : null)
                .blogId(blog != null ? blog.getId() : null)
                .blogTitle(blog != null ? blog.getTitle() : null)
                .blogContent(blog != null ? blog.getContent() : null)
                .blogThumbnailUrl(blog != null ? blog.getThumbnailUrl() : null)
                .blogStatus(blog != null ? blog.getStatus() : null)
                .blogAuthorId(blog != null && blog.getAuthor() != null ? blog.getAuthor().getUserId() : null)
                .blogAuthorName(blog != null && blog.getAuthor() != null ? displayName(blog.getAuthor()) : null)
                .build();
    }

    private ReplyResponse toReplyResponse(CommentReply reply, Long currentUserId) {
        User user = reply.getUser();
        User mentioned = reply.getMentionedUser();
        Long replyId = reply.getId();

        return ReplyResponse.builder()
                .id(replyId)
                .commentId(reply.getComment() != null ? reply.getComment().getId() : null)
                .userId(user != null ? user.getUserId() : null)
                .userName(user != null ? displayName(user) : null)
                .userAvatar(user != null ? user.getAvatarUrl() : null)
                .mentionedUserId(mentioned != null ? mentioned.getUserId() : null)
                .mentionedUserName(mentioned != null ? displayName(mentioned) : null)
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .likeCount(replyLikeRepository.countByReply_Id(replyId))
                .likedByMe(currentUserId != null
                        && replyLikeRepository.existsByReply_IdAndUser_UserId(replyId, currentUserId))
                .build();
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Noi dung khong duoc de trong");
        }
        return content.trim();
    }

    private BlogShare.ShareType parseShareType(String shareType) {
        try {
            return BlogShare.ShareType.valueOf(shareType == null ? "" : shareType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "shareType phai la COPY_URL hoac PROFILE");
        }
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }
}
