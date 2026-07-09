package com.vslbackend.service.inter;

import com.vslbackend.dto.request.admin.AdminCreateBlogRequest;
import com.vslbackend.dto.request.admin.AdminUpdateBlogRequest;
import com.vslbackend.dto.request.user.UserCreateBlogRequest;
import com.vslbackend.dto.request.user.UserUpdateBlogRequest;
import com.vslbackend.dto.response.BlogResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {
    Page<BlogResponse> getAllBlogs(int page, int size);
    Page<BlogResponse> getPublishedBlogs(int page, int size);
    BlogResponse getBlogById(Long id);
    BlogResponse createBlog(AdminCreateBlogRequest request, Long authorId);
    BlogResponse updateBlog(Long id, AdminUpdateBlogRequest request);
    void deleteBlog(Long id);
    String uploadThumbnail(Long id, MultipartFile image);

    // User-specific methods
    Page<BlogResponse> getUserBlogs(Long userId, int page, int size);
    BlogResponse createUserBlog(UserCreateBlogRequest request, Long authorId);
    BlogResponse updateUserBlog(Long blogId, Long userId, UserUpdateBlogRequest request);
    void deleteUserBlog(Long blogId, Long userId);
    String uploadUserBlogThumbnail(Long blogId, Long userId, MultipartFile image);
}
