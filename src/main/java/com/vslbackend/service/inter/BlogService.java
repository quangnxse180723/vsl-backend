package com.vslbackend.service.inter;

import com.vslbackend.dto.request.admin.AdminCreateBlogRequest;
import com.vslbackend.dto.request.admin.AdminUpdateBlogRequest;
import com.vslbackend.dto.request.user.UserCreateBlogRequest;
import com.vslbackend.dto.request.user.UserUpdateBlogRequest;
import com.vslbackend.dto.response.BlogSearchResponse;
import com.vslbackend.dto.response.BlogResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {
    /** Danh sach blog cho admin: bai nhap chi hien cua chinh admin dang xem. */
    Page<BlogResponse> getAllBlogs(int page, int size, Long adminId);
    Page<BlogResponse> getPublishedBlogs(int page, int size, Long currentUserId);
    BlogSearchResponse searchPublishedBlogsAndUsers(String keyword, int page, int size, Long currentUserId);
    Page<BlogResponse> getPublishedBlogsByUser(Long userId, int page, int size, Long currentUserId);
    /** Chi tiet cong khai: an bai da bi go (REMOVED). */
    BlogResponse getPublicBlogById(Long id, Long currentUserId);
    /** Chi tiet noi bo (admin) - tra ve bat ke trang thai. */
    BlogResponse getBlogById(Long id);
    BlogResponse createBlog(AdminCreateBlogRequest request, Long authorId);
    BlogResponse updateBlog(Long id, AdminUpdateBlogRequest request);
    void deleteBlog(Long id);
    /** Admin go bai (do vi pham) - chuyen REMOVED + luu ly do cho tac gia. */
    void removeBlogByAdmin(Long id, String reason);
    String uploadThumbnail(Long id, MultipartFile image);



    // User-specific methods
    Page<BlogResponse> getUserBlogs(Long userId, int page, int size);
    BlogResponse createUserBlog(UserCreateBlogRequest request, Long authorId);
    BlogResponse updateUserBlog(Long blogId, Long userId, UserUpdateBlogRequest request);
    void deleteUserBlog(Long blogId, Long userId);
    String uploadUserBlogThumbnail(Long blogId, Long userId, MultipartFile image);
    Page<BlogResponse> getSharedBlogsOnProfile(Long userId, int page, int size);
}
