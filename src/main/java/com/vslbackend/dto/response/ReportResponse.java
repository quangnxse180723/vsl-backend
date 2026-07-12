package com.vslbackend.dto.response;

import com.vslbackend.entity.BlogStatus;
import com.vslbackend.entity.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportResponse {
    private Long id;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    // Nguoi gui report
    private Long reporterId;
    private String reporterName;

    // Bai bi to cao - kem ID de admin tim & doc nhanh
    private Long blogId;
    private String blogTitle;
    private String blogContent;
    private String blogThumbnailUrl;
    private BlogStatus blogStatus;
    private Long blogAuthorId;
    private String blogAuthorName;
}
