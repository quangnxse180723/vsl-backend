package com.vslbackend.controller;

import com.vslbackend.dto.request.user.UserCreateBlogRequest;
import com.vslbackend.dto.request.user.UserUpdateBlogRequest;
import com.vslbackend.dto.response.BlogResponse;
import com.vslbackend.security.CustomUserDetails;
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

@RestController
@RequestMapping("/api/user/blogs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserBlogController {

    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<Page<BlogResponse>> getUserBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.getUserBlogs(principal.getUser().getUserId(), page, size));
    }

    @PostMapping
    public ResponseEntity<BlogResponse> createUserBlog(
            @Valid @RequestBody UserCreateBlogRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.createUserBlog(request, principal.getUser().getUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateUserBlog(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateBlogRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.updateUserBlog(id, principal.getUser().getUserId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUserBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        blogService.deleteUserBlog(id, principal.getUser().getUserId());
        return ResponseEntity.ok("Blog deleted successfully");
    }

    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadThumbnail(
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.uploadUserBlogThumbnail(id, principal.getUser().getUserId(), image));
    }

    @GetMapping("/shared/me")
    public ResponseEntity<Page<BlogResponse>> getSharedBlogs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(blogService.getSharedBlogsOnProfile(principal.getUser().getUserId(), page, size));
    }
}
