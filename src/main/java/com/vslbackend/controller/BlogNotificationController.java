package com.vslbackend.controller;

import com.vslbackend.dto.response.BlogNotificationResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.BlogNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blogs/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class BlogNotificationController {

    private final BlogNotificationService blogNotificationService;

    @GetMapping
    public ResponseEntity<Page<BlogNotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(blogNotificationService.getNotifications(principal.getUser().getUserId(), pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogNotificationService.countUnread(principal.getUser().getUserId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<BlogNotificationResponse> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(blogNotificationService.markRead(id, principal.getUser().getUserId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<String> markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        blogNotificationService.markAllRead(principal.getUser().getUserId());
        return ResponseEntity.ok("Notifications marked as read");
    }
}
