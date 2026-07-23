package com.vslbackend.service.inter;

import com.vslbackend.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BlogEngagementService {

    // ── User interactions ──
    LikeResponse toggleLike(Long blogId, Long userId);
    List<CommentResponse> getComments(Long blogId);
    /** Danh sach nguoi da thich bai (admin xem chi tiet). */
    List<LikeUserResponse> getLikers(Long blogId);
    CommentResponse addComment(Long blogId, Long userId, String content);
    void deleteComment(Long commentId, Long userId);
    void reportBlog(Long blogId, Long userId, String reason);

    //reply
    ReplyResponse addReply(Long commentId, Long userId, Long mentionedUserId, String content);
    Page<ReplyResponse> getReplies(Long commentId, Pageable pageable);
    void deleteReply(Long replyId, Long userId);

    // share
    ShareResponse shareBlog(Long blogId, Long userId, String shareType);


    // ── Admin report handling ──
    Page<ReportResponse> getReports(int page, int size);
    long countPendingReports();
    void resolveReport(Long reportId);
}
