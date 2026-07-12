package com.vslbackend.controller;

import com.vslbackend.dto.response.BlogResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    private Long uid(CustomUserDetails principal) {
        return principal != null ? principal.getUser().getUserId() : null;
    }

    @GetMapping
    public ResponseEntity<Page<BlogResponse>> getPublishedBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.getPublishedBlogs(page, size, uid(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogService.getPublicBlogById(id, uid(principal)));
    }
}
