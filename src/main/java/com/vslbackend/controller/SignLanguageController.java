package com.vslbackend.controller;

import com.vslbackend.dto.request.CreateVocabularyRequest;
import com.vslbackend.dto.response.ApiResponse;
import com.vslbackend.dto.response.EvaluationResponse;
import com.vslbackend.dto.response.UserProgressResponse;
import com.vslbackend.dto.response.VocabularyResponse;
import com.vslbackend.entity.Category;
import com.vslbackend.entity.UserProgress;
import com.vslbackend.entity.Vocabulary;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.CategoryRepository;
import com.vslbackend.repository.UserProgressRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.MinioService;
import com.vslbackend.service.inter.SignLanguageEvaluationService;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SignLanguageController {

    private final SignLanguageEvaluationService evaluationService;
    private final MinioService minioService;
    private final VocabularyRepository vocabularyRepository;
    private final CategoryRepository categoryRepository;
    private final UserProgressRepository userProgressRepository;

    // =====================================================================
    //  PRACTICE - bat buoc JWT (anyRequest().authenticated() trong SecurityConfig)
    // =====================================================================

    /**
     * Danh gia video thuc hanh ky hieu ngon ngu.
     *
     * <pre>
     * POST /api/practice/evaluate
     * Content-Type: multipart/form-data
     * Authorization: Bearer {accessToken}
     *
     * Form fields:
     *   video      (required) - file video thuc hanh (.mp4 / .webm / .mov)
     *   expectedId (required) - chi so class ONNX cua tu vung can luyen tap
     * </pre>
     */
    /**
     * Nhan video va tra ve CompletableFuture.
     * Spring MVC (Spring 6+) giu request "mo" cho den khi Future hoan thanh,
     * sau do flush response — Tomcat thread duoc nha ngay lap tuc sau khi Future duoc tra ve.
     * Exception tu @Async duoc Spring unwrap va routing qua GlobalExceptionHandler.
     */
    @PostMapping(value = "/api/practice/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<ApiResponse<EvaluationResponse>>> evaluate(
            @RequestPart("video") MultipartFile video,
            @RequestParam("expectedId") int expectedId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getUser().getUserId();
        log.info("Evaluate request queued: userId={}, expectedId={}, fileSize={}KB",
                userId, expectedId, video.getSize() / 1024);

        return evaluationService.evaluate(video, expectedId, userId)
                .thenApply(result -> ResponseEntity.ok(ApiResponse.of("Danh gia hoan tat", result)));
    }

    /**
     * Xem tien trinh hoc (LEARNING/LEARNED) cua nguoi dung dang dang nhap,
     * cho tung tu vung da tung luyen tap. Tu vung chua tung luyen tap (chua
     * co attempt nao) se khong xuat hien trong danh sach nay.
     *
     * <pre>
     * GET /api/practice/progress
     * Authorization: Bearer {accessToken}
     * </pre>
     */
    @GetMapping("/api/practice/progress")
    public ResponseEntity<ApiResponse<List<UserProgressResponse>>> getMyProgress(
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getUser().getUserId();
        List<UserProgress> progresses = userProgressRepository
                .findAllWithVocabularyByUserId(userId);

        List<UserProgressResponse> response = progresses.stream()
                .map(p -> UserProgressResponse.builder()
                        .vocabularyId(p.getVocabulary().getId())
                        .word(p.getVocabulary().getWord())
                        .categoryName(p.getVocabulary().getCategory().getName())
                        .learningStatus(p.getLearningStatus())
                        .lastAttemptedAt(p.getLastAttemptedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.of("Lay tien trinh thanh cong", response));
    }

    // =====================================================================
    //  ADMIN - quan ly tu vung (chi ADMIN)
    // =====================================================================

    /**
     * Tao moi mot tu vung (chua co video). Goi tiep endpoint upload
     * tutorial-video voi id tra ve de gan video.
     *
     * <pre>
     * POST /api/admin/vocabulary
     * Content-Type: application/json
     * Authorization: Bearer {adminToken}
     *
     * Body:
     *   { "categoryId": 1, "word": "con cho", "description": null }
     * </pre>
     */
    @PostMapping(value = "/api/admin/vocabulary", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VocabularyResponse>> createVocabulary(
            @Valid @RequestBody CreateVocabularyRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Vocabulary vocabulary = Vocabulary.builder()
                .category(category)
                .word(request.getWord())
                .description(request.getDescription())
                .build();
        vocabulary = vocabularyRepository.save(vocabulary);

        log.info("Admin created vocabulary id={}, word='{}'", vocabulary.getId(), vocabulary.getWord());

        VocabularyResponse response = VocabularyResponse.builder()
                .id(vocabulary.getId())
                .categoryId(category.getId())
                .categoryName(category.getName())
                .word(vocabulary.getWord())
                .description(vocabulary.getDescription())
                .videoTutorialUrl(vocabulary.getVideoTutorialUrl())
                .expectedId(vocabulary.getExpectedId())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Tao tu vung thanh cong", response));
    }

    /**
     * Upload video bai hoc mau len MinIO va luu URL vao Vocabulary.
     *
     * <pre>
     * POST /api/admin/vocabulary/{vocabularyId}/tutorial-video
     * Content-Type: multipart/form-data
     * Authorization: Bearer {adminToken}
     *
     * Form fields:
     *   video      (required) - file video mau (.mp4)
     *   expectedId (required) - chi so class trong ONNX model
     * </pre>
     */
    @PostMapping(value = "/api/admin/vocabulary/{vocabularyId}/tutorial-video",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadTutorialVideo(
            @PathVariable Long vocabularyId,
            @RequestPart("video") MultipartFile video,
            @RequestParam("expectedId") int expectedId) {

        Vocabulary vocabulary = vocabularyRepository.findById(vocabularyId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_NOT_FOUND));

        String objectName = minioService.generateObjectName(vocabularyId, video.getOriginalFilename());
        String publicUrl  = minioService.uploadTutorialVideo(video, objectName);

        vocabulary.setVideoTutorialUrl(publicUrl);
        vocabulary.setExpectedId(expectedId);
        vocabularyRepository.save(vocabulary);

        log.info("Admin uploaded tutorial video for vocabularyId={}, url={}", vocabularyId, publicUrl);
        return ResponseEntity.ok(ApiResponse.of("Upload thanh cong", publicUrl));
    }
}
