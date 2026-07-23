package com.vslbackend.service.impl;

import com.vslbackend.dto.request.CreateVocabularyRequest;
import com.vslbackend.dto.response.VocabularyResponse;
import com.vslbackend.entity.Category;
import com.vslbackend.entity.Vocabulary;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.AttemptHistoryRepository;
import com.vslbackend.repository.CategoryRepository;
import com.vslbackend.repository.UserProgressRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.MinioService;
import com.vslbackend.service.VideoTranscodingService;
import com.vslbackend.service.inter.VocabularyService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final CategoryRepository categoryRepository;
    private final UserProgressRepository userProgressRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final MinioService minioService;
    private final VideoTranscodingService videoTranscodingService;

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularyResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vocabularyRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VocabularyResponse getById(@NonNull Long id) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        return toResponse(vocabulary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularyResponse> search(@NonNull String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vocabularyRepository.findByWordContainingIgnoreCase(keyword, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularyResponse> getByCategory(@NonNull Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vocabularyRepository.findByCategoryId(categoryId, pageable)
                .map(this::toResponse);
    }

    @Override
    public VocabularyResponse create(CreateVocabularyRequest request) {
        Category category = getCategoryOrThrow(request.getCategoryId());

        Vocabulary vocabulary = Vocabulary.builder()
                .category(category)
                .word(request.getWord())
                .description(request.getDescription())
                .build();
        vocabulary = vocabularyRepository.save(vocabulary);

        log.info("Admin created vocabulary id={}, word='{}'", vocabulary.getId(), vocabulary.getWord());
        return toResponse(vocabulary);
    }

    @Override
    public VocabularyResponse update(Long id, CreateVocabularyRequest request) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);
        Category category = getCategoryOrThrow(request.getCategoryId());

        vocabulary.setCategory(category);
        vocabulary.setWord(request.getWord());
        vocabulary.setDescription(request.getDescription());
        vocabulary = vocabularyRepository.save(vocabulary);

        log.info("Admin updated vocabulary id={}, word='{}'", vocabulary.getId(), vocabulary.getWord());
        return toResponse(vocabulary);
    }

    @Override
    public String uploadTutorialVideo(Long vocabularyId, MultipartFile video, int expectedId) {
        Vocabulary vocabulary = getVocabularyOrThrow(vocabularyId);

        File tempInput = null;
        File tempOutput = null;
        try {
            tempInput = File.createTempFile("vsl_video_upload_", ".tmp");
            video.transferTo(tempInput);

            tempOutput = videoTranscodingService.transcodeToH264(tempInput);
            String objectName = minioService.generateObjectName(vocabularyId, "video.mp4");
            String publicUrl = minioService.uploadTutorialVideo(tempOutput, objectName);

            vocabulary.setVideoTutorialUrl(publicUrl);
            vocabulary.setExpectedId(expectedId);
            vocabularyRepository.save(vocabulary);

            log.info("Admin uploaded tutorial video for vocabularyId={}, url={}", vocabularyId, publicUrl);
            return publicUrl;
        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR, "Khong the doc file tai len: " + e.getMessage());
        } finally {
            if (tempInput != null) tempInput.delete();
            if (tempOutput != null) tempOutput.delete();
        }
    }

    @Override
    public String uploadVocabularyImage(Long vocabularyId, MultipartFile image) {
        Vocabulary vocabulary = getVocabularyOrThrow(vocabularyId);

        minioService.deleteVocabularyImageByUrl(vocabulary.getImageUrl());

        String objectName = minioService.generateImageObjectName(vocabularyId, image.getOriginalFilename());
        String publicUrl = minioService.uploadVocabularyImage(image, objectName);

        vocabulary.setImageUrl(publicUrl);
        vocabularyRepository.save(vocabulary);

        log.info("Admin uploaded image for vocabularyId={}, url={}", vocabularyId, publicUrl);
        return publicUrl;
    }

    @Override
    public void delete(Long id) {
        Vocabulary vocabulary = getVocabularyOrThrow(id);

        if (attemptHistoryRepository.existsByVocabulary_Id(id) || userProgressRepository.existsByVocabulary_Id(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Khong the xoa tu vung vi da co lich su luyen tap hoac tien trinh hoc gan voi tu vung nay");
        }

        minioService.deleteTutorialVideoByUrl(vocabulary.getVideoTutorialUrl());
        minioService.deleteVocabularyImageByUrl(vocabulary.getImageUrl());
        vocabularyRepository.delete(vocabulary);

        log.info("Admin deleted vocabulary id={}", id);
    }

    private Vocabulary getVocabularyOrThrow(Long id) {
        return vocabularyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_NOT_FOUND));
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private VocabularyResponse toResponse(Vocabulary vocabulary) {
        Category category = vocabulary.getCategory();
        return VocabularyResponse.builder()
                .id(vocabulary.getId())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .word(vocabulary.getWord())
                .description(vocabulary.getDescription())
                .videoTutorialUrl(vocabulary.getVideoTutorialUrl())
                .imageUrl(vocabulary.getImageUrl())
                .expectedId(vocabulary.getExpectedId())
                .build();
    }
}
