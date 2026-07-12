package com.vslbackend.controller;

import com.vslbackend.dto.request.admin.AdminCreateBlogRequest;
import com.vslbackend.dto.request.admin.AdminUpdateBlogRequest;
import com.vslbackend.dto.response.BlogResponse;
import com.vslbackend.dto.response.CommentResponse;
import com.vslbackend.dto.response.LikeUserResponse;
import com.vslbackend.service.inter.BlogEngagementService;
import com.vslbackend.service.inter.BlogService;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vslbackend.repository.UserRepository;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;

@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlogController {

    private final BlogService blogService;
    private final BlogEngagementService engagementService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<BlogResponse>> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long adminId = resolveUserId(authentication);
        return ResponseEntity.ok(blogService.getAllBlogs(page, size, adminId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.getBlogById(id));
    }

    /** Danh sach binh luan that cua bai (admin xem chi tiet). */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getBlogComments(@PathVariable Long id) {
        return ResponseEntity.ok(engagementService.getComments(id));
    }

    /** Danh sach nguoi da thich bai (admin xem chi tiet). */
    @GetMapping("/{id}/likes")
    public ResponseEntity<List<LikeUserResponse>> getBlogLikers(@PathVariable Long id) {
        return ResponseEntity.ok(engagementService.getLikers(id));
    }

    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(
            @Valid @RequestBody AdminCreateBlogRequest request,
            Authentication authentication) {
        Long authorId = resolveUserId(authentication);
        return ResponseEntity.ok(blogService.createBlog(request, authorId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateBlogRequest request) {
        return ResponseEntity.ok(blogService.updateBlog(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.ok("Blog deleted successfully");
    }

    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadThumbnail(
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(blogService.uploadThumbnail(id, image));
    }

    // ──────── helpers ────────

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getUserId();
    }
}
