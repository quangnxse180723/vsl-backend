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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vslbackend.repository.UserRepository;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;

@RestController
@RequestMapping("/api/user/blogs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserBlogController {

    private final BlogService blogService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<BlogResponse>> getUserBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(blogService.getUserBlogs(userId, page, size));
    }

    @PostMapping
    public ResponseEntity<BlogResponse> createUserBlog(
            @Valid @RequestBody UserCreateBlogRequest request,
            Authentication authentication) {
        Long authorId = resolveUserId(authentication);
        return ResponseEntity.ok(blogService.createUserBlog(request, authorId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateUserBlog(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateBlogRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(blogService.updateUserBlog(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUserBlog(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        blogService.deleteUserBlog(id, userId);
        return ResponseEntity.ok("Blog deleted successfully");
    }

    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadThumbnail(
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(blogService.uploadUserBlogThumbnail(id, userId, image));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getUserId();
    }

    @GetMapping("/shared/me")
    public ResponseEntity<Page<BlogResponse>> getSharedBlogs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(blogService.getSharedBlogsOnProfile(principal.getUser().getUserId(), page, size));
    }
}
