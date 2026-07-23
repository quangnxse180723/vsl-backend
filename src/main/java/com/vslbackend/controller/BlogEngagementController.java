package com.vslbackend.controller;

import com.vslbackend.dto.request.user.CreateCommentRequest;
import com.vslbackend.dto.request.user.CreateReportRequest;
import com.vslbackend.dto.response.CommentResponse;
import com.vslbackend.dto.response.LikeResponse;
import com.vslbackend.dto.response.ReplyResponse;
import com.vslbackend.dto.response.ShareResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.BlogEngagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final BlogEngagementService blogEngagementService;

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

    // thêm reply cho comment
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<ReplyResponse> addReply(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long mentionedUserId,
            @RequestParam String content
    ) {
        ReplyResponse response = engagementService.addReply(
                commentId,
                principal.getUser().getUserId(),
                mentionedUserId,
                content
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Lấy danh sách reply có phân trang
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Page<ReplyResponse>> getReplies(
            @PathVariable Long commentId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        Page<ReplyResponse> response =
                engagementService.getReplies(commentId, pageable);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        engagementService.deleteReply(replyId, principal.getUser().getUserId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{blogId}/share")
    public ResponseEntity<ShareResponse> shareBlog(
            @PathVariable Long blogId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String shareType) {  // "COPY_URL" hoặc "PROFILE"

        return ResponseEntity.ok(blogEngagementService.shareBlog(blogId, principal.getUser().getUserId(), shareType));
    }

}
