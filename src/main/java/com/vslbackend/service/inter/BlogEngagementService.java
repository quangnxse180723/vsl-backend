package com.vslbackend.service.inter;

import com.vslbackend.dto.response.CommentResponse;
import com.vslbackend.dto.response.LikeResponse;
import com.vslbackend.dto.response.LikeUserResponse;
import com.vslbackend.dto.response.ReportResponse;
import org.springframework.data.domain.Page;

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

    // ── Admin report handling ──
    Page<ReportResponse> getReports(int page, int size);
    long countPendingReports();
    void resolveReport(Long reportId);
}
