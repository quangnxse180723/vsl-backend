package com.vslbackend.service.impl;

import com.vslbackend.dto.request.admin.AdminCreateBlogRequest;
import com.vslbackend.dto.request.admin.AdminUpdateBlogRequest;
import com.vslbackend.dto.request.user.UserCreateBlogRequest;
import com.vslbackend.dto.request.user.UserUpdateBlogRequest;
import com.vslbackend.dto.response.BlogResponse;
import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogReport;
import com.vslbackend.entity.BlogStatus;
import com.vslbackend.entity.ReportStatus;
import com.vslbackend.entity.User;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.BlogCommentRepository;
import com.vslbackend.repository.BlogLikeRepository;
import com.vslbackend.repository.BlogReportRepository;
import com.vslbackend.repository.BlogRepository;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.service.GeminiModerationService;
import com.vslbackend.service.MinioService;
import com.vslbackend.service.inter.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final GeminiModerationService geminiModerationService;
    private final BlogLikeRepository blogLikeRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final BlogReportRepository blogReportRepository;

    private BlogResponse toResponse(Blog blog, Long currentUserId) {
        boolean likedByMe = currentUserId != null
                && blogLikeRepository.existsByBlog_IdAndUser_UserId(blog.getId(), currentUserId);
        return BlogResponse.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .content(blog.getContent())
                .thumbnailUrl(blog.getThumbnailUrl())
                .status(blog.getStatus())
                .authorId(blog.getAuthor() != null ? blog.getAuthor().getUserId() : null)
                .authorName(blog.getAuthor() != null ? blog.getAuthor().getFullName() : null)
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .likeCount(blogLikeRepository.countByBlog_Id(blog.getId()))
                .commentCount(blogCommentRepository.countByBlog_Id(blog.getId()))
                .likedByMe(likedByMe)
                .deletionReason(blog.getStatus() == BlogStatus.REMOVED ? blog.getDeletionReason() : null)
                .build();
    }

    /** Kiem duyet AI - nem BLOG_REJECTED kem ly do neu khong dat. */
    private void moderateOrThrow(String title, String content) {
        GeminiModerationService.ModerationResult result = geminiModerationService.moderate(title, content);
        if (!result.allowed()) {
            String reason = (result.reason() == null || result.reason().isBlank())
                    ? "Noi dung khong phu hop voi tieu chuan cong dong."
                    : result.reason();
            throw new AppException(ErrorCode.BLOG_REJECTED, reason);
        }
    }

    @Override
    public Page<BlogResponse> getAllBlogs(int page, int size, Long adminId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Bai nhap (DRAFT) cua nguoi khac bi loai; admin chi thay nhap cua chinh minh.
        return blogRepository.findAllForAdmin(adminId, pageable).map(b -> toResponse(b, null));
    }

    @Override
    public Page<BlogResponse> getPublishedBlogs(int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blogRepository.findByStatus(BlogStatus.PUBLISHED, pageable)
                .map(b -> toResponse(b, currentUserId));
    }

    @Override
    public BlogResponse getPublicBlogById(Long id, Long currentUserId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        // Bai bi go hoac con la nhap thi khong hien cong khai
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }
        return toResponse(blog, currentUserId);
    }

    @Override
    public BlogResponse getBlogById(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        return toResponse(blog, null);
    }

    @Override
    public BlogResponse createBlog(AdminCreateBlogRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BlogStatus status = parseStatus(request.getStatus());

        Blog blog = Blog.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(status)
                .author(author)
                .build();

        return toResponse(blogRepository.save(blog), null);
    }

    @Override
    public BlogResponse updateBlog(Long id, AdminUpdateBlogRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());

        if (request.getStatus() != null) {
            try {
                blog.setStatus(BlogStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return toResponse(blogRepository.save(blog), null);
    }

    @Override
    public void deleteBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        hardDelete(blog);
    }

    @Override
    public void removeBlogByAdmin(Long id, String reason) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blog.setStatus(BlogStatus.REMOVED);
        blog.setDeletionReason(reason);
        blogRepository.save(blog);

        // Danh dau moi report cua bai nay la da xu ly
        LocalDateTime now = LocalDateTime.now();
        for (BlogReport report : blogReportRepository.findByBlog_Id(id)) {
            if (report.getStatus() == ReportStatus.PENDING) {
                report.setStatus(ReportStatus.RESOLVED);
                report.setResolvedAt(now);
                blogReportRepository.save(report);
            }
        }
    }

    @Override
    public String uploadThumbnail(Long id, MultipartFile image) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        return doUploadThumbnail(blog, image);
    }

    // ──────────────────────── USER SPECIFIC METHODS ────────────────────────

    @Override
    public Page<BlogResponse> getUserBlogs(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blogRepository.findByAuthor_UserId(userId, pageable)
                .map(b -> toResponse(b, userId));
    }

    @Override
    public BlogResponse createUserBlog(UserCreateBlogRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BlogStatus status = parseStatus(request.getStatus());

        // Kiem duyet AI khi bai duoc dang cong khai (PUBLISHED). Nhap (DRAFT) thi bo qua.
        if (status == BlogStatus.PUBLISHED) {
            moderateOrThrow(request.getTitle(), request.getContent());
        }

        Blog blog = Blog.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(status)
                .author(author)
                .build();

        return toResponse(blogRepository.save(blog), authorId);
    }

    @Override
    public BlogResponse updateUserBlog(Long blogId, Long userId, UserUpdateBlogRequest request) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getAuthor() == null || !blog.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen sua bai viet nay");
        }
        if (blog.getStatus() == BlogStatus.REMOVED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bai viet da bi go, khong the chinh sua");
        }

        BlogStatus newStatus = request.getStatus() != null ? parseStatus(request.getStatus()) : blog.getStatus();

        // Kiem duyet lai khi noi dung se duoc dang cong khai
        if (newStatus == BlogStatus.PUBLISHED) {
            moderateOrThrow(request.getTitle(), request.getContent());
        }

        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setStatus(newStatus);

        return toResponse(blogRepository.save(blog), userId);
    }

    @Override
    public void deleteUserBlog(Long blogId, Long userId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getAuthor() == null || !blog.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen xoa bai viet nay");
        }

        hardDelete(blog);
    }

    @Override
    public String uploadUserBlogThumbnail(Long blogId, Long userId, MultipartFile image) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getAuthor() == null || !blog.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen cap nhat bai viet nay");
        }

        return doUploadThumbnail(blog, image);
    }

    // ──────────────────────── HELPERS ────────────────────────

    private BlogStatus parseStatus(String raw) {
        try {
            return BlogStatus.valueOf(raw != null ? raw.toUpperCase() : "DRAFT");
        } catch (IllegalArgumentException e) {
            return BlogStatus.DRAFT;
        }
    }

    /** Xoa cung 1 bai + toan bo like/comment/report/thumbnail lien quan. */
    private void hardDelete(Blog blog) {
        blogLikeRepository.deleteByBlog_Id(blog.getId());
        blogCommentRepository.deleteByBlog_Id(blog.getId());
        blogReportRepository.deleteByBlog_Id(blog.getId());
        if (blog.getThumbnailUrl() != null) {
            minioService.deleteBlogThumbnailByUrl(blog.getThumbnailUrl());
        }
        blogRepository.delete(blog);
    }

    private String doUploadThumbnail(Blog blog, MultipartFile image) {
        if (blog.getThumbnailUrl() != null) {
            minioService.deleteBlogThumbnailByUrl(blog.getThumbnailUrl());
        }
        String objectName = "blog-" + blog.getId() + "/" + UUID.randomUUID() + extractExt(image.getOriginalFilename());
        String publicUrl = minioService.uploadBlogThumbnail(image, objectName);
        blog.setThumbnailUrl(publicUrl);
        blogRepository.save(blog);
        return publicUrl;
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
