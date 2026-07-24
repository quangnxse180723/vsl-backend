package com.vslbackend.controller;

import com.vslbackend.dto.request.CreateVocabularyRequest;
import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.EvaluationResponse;
import com.vslbackend.dto.response.UserProgressResponse;
import com.vslbackend.dto.response.VocabularyResponse;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.inter.PracticeService;
import com.vslbackend.service.inter.SignLanguageEvaluationService;
import com.vslbackend.service.inter.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SignLanguageController {

    private final SignLanguageEvaluationService evaluationService;
    private final PracticeService practiceService;
    private final VocabularyService vocabularyService;

    @PostMapping(value = "/api/practice/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<ApiResponse<EvaluationResponse>>> evaluate(
            @RequestPart("video") MultipartFile video,
            @RequestParam("expectedId") int expectedId,
            @RequestParam(value = "startFrac", required = false, defaultValue = "0.0") float startFrac,
            @RequestParam(value = "endFrac", required = false, defaultValue = "1.0") float endFrac,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getUser().getUserId();
        log.info("Evaluate request queued: userId={}, expectedId={}, fileSize={}KB, window=[{},{}]",
                userId, expectedId, video.getSize() / 1024, startFrac, endFrac);

        return evaluationService.evaluate(video, expectedId, userId, startFrac, endFrac)
                .thenApply(result -> ResponseEntity.ok(ApiResponse.of("Danh gia hoan tat", result)));
    }

    @GetMapping("/api/practice/progress")
    public ResponseEntity<ApiResponse<List<UserProgressResponse>>> getMyProgress(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.of(
                "Lay tien trinh thanh cong",
                practiceService.getMyProgress(principal.getUser().getUserId())));
    }

    @PostMapping(value = "/api/admin/vocabulary", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VocabularyResponse>> createVocabulary(
            @Valid @RequestBody CreateVocabularyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Tao tu vung thanh cong", vocabularyService.create(request)));
    }

    @PostMapping(value = "/api/admin/vocabulary/{vocabularyId}/tutorial-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadTutorialVideo(
            @PathVariable Long vocabularyId,
            @RequestPart("video") MultipartFile video,
            @RequestParam("expectedId") int expectedId) {
        return ResponseEntity.ok(ApiResponse.of(
                "Upload thanh cong",
                vocabularyService.uploadTutorialVideo(vocabularyId, video, expectedId)));
    }

    @PostMapping(value = "/api/admin/vocabulary/{vocabularyId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadVocabularyImage(
            @PathVariable Long vocabularyId,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.of(
                "Upload anh thanh cong",
                vocabularyService.uploadVocabularyImage(vocabularyId, image)));
    }

    @PutMapping(value = "/api/admin/vocabulary/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VocabularyResponse>> updateVocabulary(
            @PathVariable Long id,
            @Valid @RequestBody CreateVocabularyRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                "Cap nhat tu vung thanh cong",
                vocabularyService.update(id, request)));
    }

    @DeleteMapping("/api/admin/vocabulary/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVocabulary(@PathVariable Long id) {
        vocabularyService.delete(id);
        return ResponseEntity.ok(ApiResponse.of("Xoa tu vung thanh cong"));
    }
}
