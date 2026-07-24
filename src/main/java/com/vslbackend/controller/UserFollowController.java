package com.vslbackend.controller;

import com.vslbackend.dto.response.FollowStatusResponse;
import com.vslbackend.dto.response.UserSummaryResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.FollowService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserFollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public ResponseEntity<FollowStatusResponse> follow(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(followService.follow(userId, principal.getUser().getUserId()));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<FollowStatusResponse> unfollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(followService.unfollow(userId, principal.getUser().getUserId()));
    }

    @GetMapping("/{userId}/follow-status")
    public ResponseEntity<FollowStatusResponse> getStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(followService.getStatus(userId, principal.getUser().getUserId()));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserSummaryResponse>> getFollowers(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowers(userId, principal.getUser().getUserId(), pageable));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserSummaryResponse>> getFollowing(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowing(userId, principal.getUser().getUserId(), pageable));
    }
}
