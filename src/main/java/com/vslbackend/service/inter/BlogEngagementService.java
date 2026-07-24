package com.vslbackend.service.inter;

import com.vslbackend.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BlogEngagementService {
    LikeResponse toggleLike(Long blogId, Long userId);
    LikeResponse toggleCommentLike(Long commentId, Long userId);
    LikeResponse toggleReplyLike(Long replyId, Long userId);
    List<CommentResponse> getComments(Long blogId, Long currentUserId);
    List<LikeUserResponse> getLikers(Long blogId);
    CommentResponse addComment(Long blogId, Long userId, Long mentionedUserId, String content);
    void deleteComment(Long commentId, Long userId);
    void reportBlog(Long blogId, Long userId, String reason);

    ReplyResponse addReply(Long commentId, Long userId, Long mentionedUserId, String content);
    Page<ReplyResponse> getReplies(Long commentId, Long currentUserId, Pageable pageable);
    void deleteReply(Long replyId, Long userId);

    ShareResponse shareBlog(Long blogId, Long userId, String shareType, Long recipientUserId);

    Page<ReportResponse> getReports(int page, int size);
    long countPendingReports();
    void resolveReport(Long reportId);
}
