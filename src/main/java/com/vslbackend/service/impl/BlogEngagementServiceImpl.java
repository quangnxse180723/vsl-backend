package com.vslbackend.service.impl;

import com.vslbackend.dto.response.CommentResponse;
import com.vslbackend.dto.response.LikeResponse;
import com.vslbackend.dto.response.LikeUserResponse;
import com.vslbackend.dto.response.ReportResponse;
import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogComment;
import com.vslbackend.entity.BlogLike;
import com.vslbackend.entity.BlogReport;
import com.vslbackend.entity.ReportStatus;
import com.vslbackend.entity.User;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.*;
import com.vslbackend.service.inter.BlogEngagementService;
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
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long blogId) {
        return blogCommentRepository.findByBlogIdWithUser(blogId).stream()
                .map(this::toCommentResponse)
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
    public CommentResponse addComment(Long blogId, Long userId, String content) {
        Blog blog = getBlogOrThrow(blogId);
        User user = getUserOrThrow(userId);

        BlogComment comment = blogCommentRepository.save(BlogComment.builder()
                .blog(blog)
                .user(user)
                .content(content)
                .build());

        return toCommentResponse(comment);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.getUser() == null || !comment.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen xoa binh luan nay");
        }
        blogCommentRepository.delete(comment);
    }

    @Override
    public void reportBlog(Long blogId, Long userId, String reason) {
        Blog blog = getBlogOrThrow(blogId);
        // Khong cho tac gia tu to cao bai cua chinh minh
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

    // ──────────────────────── ADMIN ────────────────────────

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

    // ──────────────────────── MAPPERS ────────────────────────

    private LikeUserResponse toLikeUserResponse(BlogLike l) {
        User u = l.getUser();
        return LikeUserResponse.builder()
                .userId(u != null ? u.getUserId() : null)
                .userName(u != null ? u.getFullName() : null)
                .userAvatar(u != null ? u.getAvatarUrl() : null)
                .createdAt(l.getCreatedAt())
                .build();
    }

    private CommentResponse toCommentResponse(BlogComment c) {
        User u = c.getUser();
        return CommentResponse.builder()
                .id(c.getId())
                .blogId(c.getBlog() != null ? c.getBlog().getId() : null)
                .userId(u != null ? u.getUserId() : null)
                .userName(u != null ? u.getFullName() : null)
                .userAvatar(u != null ? u.getAvatarUrl() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private ReportResponse toReportResponse(BlogReport r) {
        Blog b = r.getBlog();
        User reporter = r.getReporter();
        return ReportResponse.builder()
                .id(r.getId())
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .reporterId(reporter != null ? reporter.getUserId() : null)
                .reporterName(reporter != null ? reporter.getFullName() : null)
                .blogId(b != null ? b.getId() : null)
                .blogTitle(b != null ? b.getTitle() : null)
                .blogContent(b != null ? b.getContent() : null)
                .blogThumbnailUrl(b != null ? b.getThumbnailUrl() : null)
                .blogStatus(b != null ? b.getStatus() : null)
                .blogAuthorId(b != null && b.getAuthor() != null ? b.getAuthor().getUserId() : null)
                .blogAuthorName(b != null && b.getAuthor() != null ? b.getAuthor().getFullName() : null)
                .build();
    }
}
