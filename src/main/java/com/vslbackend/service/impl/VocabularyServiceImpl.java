package com.vslbackend.service.impl;

import com.vslbackend.dto.response.VocabularyExistsResponse;
import com.vslbackend.dto.response.VocabularyResponse;
import com.vslbackend.dto.response.VocabularySynonymResponse;
import com.vslbackend.entity.Vocabulary;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.GeminiModerationService;
import com.vslbackend.service.inter.VocabularyService;
import com.vslbackend.util.VietnameseText;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final GeminiModerationService geminiModerationService;

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

    @Override
    @Transactional(readOnly = true)
    public VocabularyExistsResponse checkWordExists(@NonNull String word) {
        return findMatchingVocab(word)
                .map(v -> VocabularyExistsResponse.builder()
                        .exists(true)
                        .word(v.getWord())
                        .categoryId(v.getCategory() != null ? v.getCategory().getId() : null)
                        .categoryName(v.getCategory() != null ? v.getCategory().getName() : null)
                        .build())
                .orElseGet(() -> VocabularyExistsResponse.builder().exists(false).build());
    }

    /**
     * Tim tu vung trung - KHONG phan biet HOA/thuong VA KHONG phan biet DAU
     * (vd input "con chuot" van khop tu da co "con chuột"). Duyet toan bo (~43 dong)
     * va so sanh o dang da bo dau. Neu tu vung phinh to (hang nghin dong) can toi uu sau.
     */
    @Transactional(readOnly = true)
    public Optional<Vocabulary> findMatchingVocab(String word) {
        String target = VietnameseText.fold(word);
        if (target.isBlank()) return Optional.empty();
        return vocabularyRepository.findAll(Pageable.unpaged()).stream()
                .filter(v -> VietnameseText.fold(v.getWord()).equals(target))
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public VocabularySynonymResponse findSynonyms(@NonNull String word) {
        String normalized = normalizeWord(word);
        // AI tat / khong co key -> tra ve aiChecked=false de UI khong hien gi.
        if (!geminiModerationService.isEnabled() || normalized.isBlank()) {
            return VocabularySynonymResponse.builder().aiChecked(false).aiError(false).synonyms(List.of()).build();
        }

        // Lay toan bo tu vung + danh muc (hien ~43 dong) de dua vao prompt.
        List<GeminiModerationService.VocabEntry> existing = vocabularyRepository.findAll(Pageable.unpaged())
                .stream()
                .map(v -> new GeminiModerationService.VocabEntry(
                        v.getWord(),
                        v.getCategory() != null ? v.getCategory().getName() : ""))
                .toList();

        try {
            List<VocabularySynonymResponse.SynonymItem> items =
                    geminiModerationService.findSynonyms(normalized, existing).stream()
                            .map(m -> VocabularySynonymResponse.SynonymItem.builder()
                                    .word(m.word())
                                    .categoryName(m.categoryName())
                                    .build())
                            .toList();
            return VocabularySynonymResponse.builder().aiChecked(true).aiError(false).synonyms(items).build();
        } catch (Exception e) {
            // Goi AI that bai (vd het quota 429) -> bao loi de UI hien thong bao, khong chan form.
            log.warn("AI synonym scan failed for '{}': {}", normalized, e.getMessage());
            return VocabularySynonymResponse.builder().aiChecked(true).aiError(true).synonyms(List.of()).build();
        }
    }

    /** Chuan hoa tu vung truoc khi so khop/luu: cat khoang trang 2 dau + gom khoang trang lien tiep. */
    public static String normalizeWord(String word) {
        return word == null ? "" : word.trim().replaceAll("\\s+", " ");
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