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
import com.vslbackend.repository.AttemptHistoryRepository;
import com.vslbackend.repository.CategoryRepository;
import com.vslbackend.repository.UserProgressRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.security.CustomUserDetails;
import com.vslbackend.service.MinioService;
import com.vslbackend.service.VideoTranscodingService;
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

import java.io.File;
import java.io.IOException;
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
    private final VideoTranscodingService videoTranscodingService;
    private final VocabularyRepository vocabularyRepository;
    private final CategoryRepository categoryRepository;
    private final UserProgressRepository userProgressRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;

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
                .imageUrl(vocabulary.getImageUrl())
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

        File tempInput = null;
        File tempOutput = null;
        try {
            tempInput = File.createTempFile("vsl_video_upload_", ".tmp");
            video.transferTo(tempInput);

            // Chuan hoa ve H.264/AAC: video nguon co the dung codec (vd mp4v) ma
            // trinh duyet khong giai ma duoc qua the <video>.
            tempOutput = videoTranscodingService.transcodeToH264(tempInput);

            String objectName = minioService.generateObjectName(vocabularyId, "video.mp4");
            String publicUrl  = minioService.uploadTutorialVideo(tempOutput, objectName);

            vocabulary.setVideoTutorialUrl(publicUrl);
            vocabulary.setExpectedId(expectedId);
            vocabularyRepository.save(vocabulary);

            log.info("Admin uploaded tutorial video for vocabularyId={}, url={}", vocabularyId, publicUrl);
            return ResponseEntity.ok(ApiResponse.of("Upload thanh cong", publicUrl));
        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR, "Khong the doc file tai len: " + e.getMessage());
        } finally {
            if (tempInput != null) tempInput.delete();
            if (tempOutput != null) tempOutput.delete();
        }
    }

    /**
     * Upload anh minh hoa tu vung len MinIO va luu URL vao Vocabulary.
     *
     * <pre>
     * POST /api/admin/vocabulary/{vocabularyId}/image
     * Content-Type: multipart/form-data
     * Authorization: Bearer {adminToken}
     *
     * Form fields:
     *   image (required) - file anh (.jpg / .png / .webp)
     * </pre>
     */
    @PostMapping(value = "/api/admin/vocabulary/{vocabularyId}/image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadVocabularyImage(
            @PathVariable Long vocabularyId,
            @RequestPart("image") MultipartFile image) {

        Vocabulary vocabulary = vocabularyRepository.findById(vocabularyId)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_NOT_FOUND));

        // Replace the previous image (if any) so the bucket doesn't accumulate orphans.
        minioService.deleteVocabularyImageByUrl(vocabulary.getImageUrl());

        String objectName = minioService.generateImageObjectName(vocabularyId, image.getOriginalFilename());
        String publicUrl  = minioService.uploadVocabularyImage(image, objectName);

        vocabulary.setImageUrl(publicUrl);
        vocabularyRepository.save(vocabulary);

        log.info("Admin uploaded image for vocabularyId={}, url={}", vocabularyId, publicUrl);
        return ResponseEntity.ok(ApiResponse.of("Upload anh thanh cong", publicUrl));
    }

    /**
     * Xoa mot tu vung (va video mau tren MinIO neu co). Chan xoa neu tu vung
     * da co lich su luyen tap hoac tien trinh hoc gan voi no, tranh mo coi du lieu.
     *
     * <pre>
     * DELETE /api/admin/vocabulary/{id}
     * Authorization: Bearer {adminToken}
     * </pre>
     */
    @DeleteMapping("/api/admin/vocabulary/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVocabulary(@PathVariable Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_NOT_FOUND));

        if (attemptHistoryRepository.existsByVocabulary_Id(id) || userProgressRepository.existsByVocabulary_Id(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Khong the xoa tu vung vi da co lich su luyen tap hoac tien trinh hoc gan voi tu vung nay");
        }

        minioService.deleteTutorialVideoByUrl(vocabulary.getVideoTutorialUrl());
        minioService.deleteVocabularyImageByUrl(vocabulary.getImageUrl());
        vocabularyRepository.delete(vocabulary);

        log.info("Admin deleted vocabulary id={}", id);
        return ResponseEntity.ok(ApiResponse.of("Xoa tu vung thanh cong"));
    }
}
