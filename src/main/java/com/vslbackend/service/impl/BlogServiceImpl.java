package com.vslbackend.service.impl;

import com.vslbackend.dto.request.admin.AdminCreateBlogRequest;
import com.vslbackend.dto.request.admin.AdminUpdateBlogRequest;
import com.vslbackend.dto.request.user.UserCreateBlogRequest;
import com.vslbackend.dto.request.user.UserUpdateBlogRequest;
import com.vslbackend.dto.response.BlogResponse;
import com.vslbackend.entity.Blog;
import com.vslbackend.entity.BlogStatus;
import com.vslbackend.entity.User;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.BlogRepository;
import com.vslbackend.repository.UserRepository;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    private BlogResponse toResponse(Blog blog) {
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
                .build();
    }

    @Override
    public Page<BlogResponse> getAllBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blogRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public Page<BlogResponse> getPublishedBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blogRepository.findByStatus(BlogStatus.PUBLISHED, pageable).map(this::toResponse);
    }

    @Override
    public BlogResponse getBlogById(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        return toResponse(blog);
    }

    @Override
    public BlogResponse createBlog(AdminCreateBlogRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BlogStatus status;
        try {
            status = BlogStatus.valueOf(request.getStatus() != null ? request.getStatus().toUpperCase() : "DRAFT");
        } catch (IllegalArgumentException e) {
            status = BlogStatus.DRAFT;
        }

        Blog blog = Blog.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(status)
                .author(author)
                .build();

        return toResponse(blogRepository.save(blog));
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
                // Giữ nguyên status cũ nếu giá trị không hợp lệ
            }
        }

        return toResponse(blogRepository.save(blog));
    }

    @Override
    public void deleteBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        // Xóa thumbnail trên MinIO nếu có
        if (blog.getThumbnailUrl() != null) {
            minioService.deleteBlogThumbnailByUrl(blog.getThumbnailUrl());
        }

        blogRepository.delete(blog);
    }

    @Override
    public String uploadThumbnail(Long id, MultipartFile image) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        // Xóa thumbnail cũ nếu có
        if (blog.getThumbnailUrl() != null) {
            minioService.deleteBlogThumbnailByUrl(blog.getThumbnailUrl());
        }

        String objectName = "blog-" + id + "/" + UUID.randomUUID() + extractExt(image.getOriginalFilename());
        String publicUrl = minioService.uploadBlogThumbnail(image, objectName);

        blog.setThumbnailUrl(publicUrl);
        blogRepository.save(blog);

        return publicUrl;
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }

    // ──────────────────────── USER SPECIFIC METHODS ────────────────────────

    @Override
    public Page<BlogResponse> getUserBlogs(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return blogRepository.findByAuthor_UserId(userId, pageable).map(this::toResponse);
    }

    @Override
    public BlogResponse createUserBlog(UserCreateBlogRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BlogStatus status;
        try {
            status = BlogStatus.valueOf(request.getStatus() != null ? request.getStatus().toUpperCase() : "DRAFT");
        } catch (IllegalArgumentException e) {
            status = BlogStatus.DRAFT;
        }

        Blog blog = Blog.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(status)
                .author(author)
                .build();

        return toResponse(blogRepository.save(blog));
    }

    @Override
    public BlogResponse updateUserBlog(Long blogId, Long userId, UserUpdateBlogRequest request) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getAuthor() == null || !blog.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen sua bai viet nay");
        }

        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());

        if (request.getStatus() != null) {
            try {
                blog.setStatus(BlogStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        return toResponse(blogRepository.save(blog));
    }

    @Override
    public void deleteUserBlog(Long blogId, Long userId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getAuthor() == null || !blog.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen xoa bai viet nay");
        }

        if (blog.getThumbnailUrl() != null) {
            minioService.deleteBlogThumbnailByUrl(blog.getThumbnailUrl());
        }

        blogRepository.delete(blog);
    }

    @Override
    public String uploadUserBlogThumbnail(Long blogId, Long userId, MultipartFile image) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getAuthor() == null || !blog.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ban khong co quyen cap nhat bai viet nay");
        }

        if (blog.getThumbnailUrl() != null) {
            minioService.deleteBlogThumbnailByUrl(blog.getThumbnailUrl());
        }

        String objectName = "blog-" + blogId + "/" + UUID.randomUUID() + extractExt(image.getOriginalFilename());
        String publicUrl = minioService.uploadBlogThumbnail(image, objectName);

        blog.setThumbnailUrl(publicUrl);
        blogRepository.save(blog);

        return publicUrl;
    }
}
