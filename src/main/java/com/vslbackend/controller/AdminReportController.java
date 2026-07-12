package com.vslbackend.controller;

import com.vslbackend.dto.request.admin.AdminRemoveBlogRequest;
import com.vslbackend.dto.response.ReportResponse;
import com.vslbackend.service.inter.BlogEngagementService;
import com.vslbackend.service.inter.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin xu ly don to cao bai blog.
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final BlogEngagementService engagementService;
    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(engagementService.getReports(page, size));
    }

    @GetMapping("/pending-count")
    public ResponseEntity<Long> pendingCount() {
        return ResponseEntity.ok(engagementService.countPendingReports());
    }

    /** Danh dau da xu ly ma khong go bai (bo qua report). */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<String> resolve(@PathVariable Long id) {
        engagementService.resolveReport(id);
        return ResponseEntity.ok("Report resolved");
    }

    /**
     * Go bai bi to cao kem ly do. Bai chuyen REMOVED (an cong khai), tac gia se
     * thay ly do o "bai cua toi". Cac report cua bai nay tu dong duoc resolve.
     */
    @PostMapping("/blogs/{blogId}/remove")
    public ResponseEntity<String> removeBlog(
            @PathVariable Long blogId,
            @Valid @RequestBody AdminRemoveBlogRequest request) {
        blogService.removeBlogByAdmin(blogId, request.getReason());
        return ResponseEntity.ok("Da go bai va thong bao ly do cho tac gia.");
    }
}
