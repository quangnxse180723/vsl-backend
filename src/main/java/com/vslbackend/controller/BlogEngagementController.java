package com.vslbackend.controller;

import com.vslbackend.dto.request.user.CreateCommentRequest;
import com.vslbackend.dto.request.user.CreateReplyRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogEngagementController {

    private final BlogEngagementService engagementService;

    private Long uid(CustomUserDetails principal) {
        return principal != null ? principal.getUser().getUserId() : null;
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(engagementService.toggleLike(id, principal.getUser().getUserId()));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<LikeResponse> toggleCommentLike(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(engagementService.toggleCommentLike(commentId, principal.getUser().getUserId()));
    }

    @PostMapping("/replies/{replyId}/like")
    public ResponseEntity<LikeResponse> toggleReplyLike(
            @PathVariable("replyId") Long replyId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(engagementService.toggleReplyLike(replyId, principal.getUser().getUserId()));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(engagementService.getComments(id, uid(principal)));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                engagementService.addComment(
                        id,
                        principal.getUser().getUserId(),
                        request.getMentionedUserId(),
                        request.getContent()));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable("id") Long id,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        engagementService.deleteComment(commentId, principal.getUser().getUserId());
        return ResponseEntity.ok("Comment deleted");
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<String> reportBlog(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        engagementService.reportBlog(id, principal.getUser().getUserId(), request.getReason());
        return ResponseEntity.ok("Da gui to cao, cam on ban. Admin se xem xet som.");
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<ReplyResponse> addReply(
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                engagementService.addReply(
                        commentId,
                        principal.getUser().getUserId(),
                        request.getMentionedUserId(),
                        request.getContent()));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<Page<ReplyResponse>> getReplies(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {
        return ResponseEntity.ok(engagementService.getReplies(commentId, uid(principal), pageable));
    }

    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable("replyId") Long replyId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        engagementService.deleteReply(replyId, principal.getUser().getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{blogId}/share")
    public ResponseEntity<ShareResponse> shareBlog(
            @PathVariable("blogId") Long blogId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "COPY_URL") String shareType,
            @RequestParam(required = false) Long recipientUserId) {
        return ResponseEntity.ok(engagementService.shareBlog(
                blogId,
                principal.getUser().getUserId(),
                shareType,
                recipientUserId));
    }
}
