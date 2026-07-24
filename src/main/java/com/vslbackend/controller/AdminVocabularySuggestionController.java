package com.vslbackend.controller;

import com.vslbackend.dto.response.VocabularySuggestionResponse;
import com.vslbackend.service.inter.VocabularySuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin xem cac de xuat tu vung do nguoi dung gui (mang tinh goi y - chi xem, khong duyet).
 */
@RestController
@RequestMapping("/api/admin/vocabulary-suggestions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVocabularySuggestionController {

    private final VocabularySuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<Page<VocabularySuggestionResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(suggestionService.getAll(page, size));
    }

    @GetMapping("/pending-count")
    public ResponseEntity<Long> pendingCount() {
        return ResponseEntity.ok(suggestionService.countPending());
    }

    /** Danh dau da xem (bo khoi danh sach cho). */
    @PutMapping("/{id}/reviewed")
    public ResponseEntity<String> markReviewed(@PathVariable Long id) {
        suggestionService.markReviewed(id);
        return ResponseEntity.ok("Suggestion marked as reviewed");
    }
}
