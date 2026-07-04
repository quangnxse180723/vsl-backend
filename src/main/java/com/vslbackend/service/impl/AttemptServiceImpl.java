package com.vslbackend.service.impl;

import com.vslbackend.dto.response.AttemptResponse;
import com.vslbackend.entity.AttemptHistory;
import com.vslbackend.entity.Category;
import com.vslbackend.entity.Vocabulary;
import com.vslbackend.repository.AttemptHistoryRepository;
import com.vslbackend.service.inter.AttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttemptServiceImpl implements AttemptService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RECENT_LIMIT = 50;

    private final AttemptHistoryRepository attemptHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AttemptResponse> getMyAttempts(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, MAX_PAGE_SIZE);

        return attemptHistoryRepository
                .findPageByUserId(userId, PageRequest.of(safePage, safeSize))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttemptResponse> getRecentAttempts(Long userId, int limit) {
        int safeLimit = clamp(limit, 1, MAX_RECENT_LIMIT);

        return attemptHistoryRepository
                .findRecentByUserId(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AttemptResponse toResponse(AttemptHistory attempt) {
        Vocabulary vocabulary = attempt.getVocabulary();
        Category category = vocabulary != null ? vocabulary.getCategory() : null;

        return AttemptResponse.builder()
                .attemptId(attempt.getId())
                .vocabularyId(vocabulary != null ? vocabulary.getId() : null)
                .word(vocabulary != null ? vocabulary.getWord() : null)
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .expectedId(vocabulary != null ? vocabulary.getExpectedId() : null)
                .isCorrect(attempt.getIsCorrect())
                .aiPredictedCode(attempt.getAiPredictedCode())
                .confidence(attempt.getConfidence())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}
