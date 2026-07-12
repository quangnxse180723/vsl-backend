package com.vslbackend.controller;

import com.vslbackend.dto.request.user.CreateCommentRequest;
import com.vslbackend.dto.request.user.CreateReportRequest;
import com.vslbackend.dto.response.CommentResponse;
import com.vslbackend.dto.response.LikeResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.BlogEngagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tuong tac cong khai voi bai blog: like / comment / report.
 * GET (danh sach comment) la cong khai; POST/DELETE yeu cau dang nhap
 * (SecurityConfig chi permitAll GET /api/blogs/**).
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogEngagementController {

    private final BlogEngagementService engagementService;

    @PostMapping("/{id}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(engagementService.toggleLike(id, principal.getUser().getUserId()));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(engagementService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(
                engagementService.addComment(id, principal.getUser().getUserId(), request.getContent()));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        engagementService.deleteComment(commentId, principal.getUser().getUserId());
        return ResponseEntity.ok("Comment deleted");
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<String> reportBlog(
            @PathVariable Long id,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        engagementService.reportBlog(id, principal.getUser().getUserId(), request.getReason());
        return ResponseEntity.ok("Da gui to cao, cam on ban. Admin se xem xet som.");
    }
}
