package com.vslbackend.service.impl;

import com.vslbackend.dto.response.VocabularyResponse;
import com.vslbackend.entity.Vocabulary;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.inter.VocabularyService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;

    @Override
    public Page<VocabularyResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vocabularyRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public VocabularyResponse getById(@NonNull Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_NOT_FOUND));
        return toResponse(vocabulary);
    }

    @Override
    public Page<VocabularyResponse> search(@NonNull String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vocabularyRepository.findByWordContainingIgnoreCase(keyword, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<VocabularyResponse> getByCategory(@NonNull Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vocabularyRepository.findByCategoryId(categoryId, pageable)
                .map(this::toResponse);
    }

    private VocabularyResponse toResponse(Vocabulary v) {
        return VocabularyResponse.builder()
                .id(v.getId())
                .categoryId(v.getCategory() != null ? v.getCategory().getId() : null)
                .categoryName(v.getCategory() != null ? v.getCategory().getName() : null)
                .word(v.getWord())
                .description(v.getDescription())
                .videoTutorialUrl(v.getVideoTutorialUrl())
                .imageUrl(v.getImageUrl())
                .expectedId(v.getExpectedId())
                .build();
    }
}