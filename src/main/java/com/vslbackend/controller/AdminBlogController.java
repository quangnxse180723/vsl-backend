package com.vslbackend.controller;

import com.vslbackend.dto.request.admin.AdminCreateBlogRequest;
import com.vslbackend.dto.request.admin.AdminUpdateBlogRequest;
import com.vslbackend.dto.response.BlogResponse;
import com.vslbackend.dto.response.CommentResponse;
import com.vslbackend.dto.response.LikeUserResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.BlogEngagementService;
import com.vslbackend.service.inter.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlogController {

    private final BlogService blogService;
    private final BlogEngagementService engagementService;

    @GetMapping
    public ResponseEntity<Page<BlogResponse>> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.getAllBlogs(page, size, principal.getUser().getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.getBlogById(id));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getBlogComments(@PathVariable Long id) {
        return ResponseEntity.ok(engagementService.getComments(id, null));
    }

    @GetMapping("/{id}/likes")
    public ResponseEntity<List<LikeUserResponse>> getBlogLikers(@PathVariable Long id) {
        return ResponseEntity.ok(engagementService.getLikers(id));
    }

    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(
            @Valid @RequestBody AdminCreateBlogRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.createBlog(request, principal.getUser().getUserId()));
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
}
