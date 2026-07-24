package com.vslbackend.service.impl;

import com.vslbackend.dto.request.user.CreateVocabularySuggestionRequest;
import com.vslbackend.dto.response.VocabularySuggestionResponse;
import com.vslbackend.entity.Category;
import com.vslbackend.entity.SuggestionStatus;
import com.vslbackend.entity.User;
import com.vslbackend.entity.VocabularySuggestion;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.CategoryRepository;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.repository.VocabularySuggestionRepository;
import com.vslbackend.service.inter.VocabularyService;
import com.vslbackend.service.inter.VocabularySuggestionService;
import com.vslbackend.util.VietnameseText;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VocabularySuggestionServiceImpl implements VocabularySuggestionService {

    private final VocabularySuggestionRepository suggestionRepository;
    private final VocabularyService vocabularyService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public VocabularySuggestionResponse submit(Long userId, CreateVocabularySuggestionRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        String word = VocabularyServiceImpl.normalizeWord(request.getWord());

        // Chan de xuat neu tu vung da ton tai - khong phan biet hoa/thuong VA khong phan biet dau
        // (vd "con chuot" bi chan vi da co "con chuột"), toan he thong.
        if (vocabularyService.checkWordExists(request.getWord()).isExists()) {
            throw new AppException(ErrorCode.VOCABULARY_ALREADY_EXISTS);
        }

        // Chan de xuat TRUNG voi mot de xuat khac dang cho duyet (cua chinh minh hoac cua nguoi
        // khac) - tranh hang doi duyet cua admin bi lap. So khop cung kieu bo dau/hoa-thuong.
        String folded = VietnameseText.fold(word);
        boolean pendingDuplicate = suggestionRepository.findByStatus(SuggestionStatus.PENDING).stream()
                .anyMatch(s -> VietnameseText.fold(s.getWord()).equals(folded));
        if (pendingDuplicate) {
            throw new AppException(ErrorCode.VOCABULARY_SUGGESTION_ALREADY_PENDING);
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        VocabularySuggestion saved = suggestionRepository.save(VocabularySuggestion.builder()
                .category(category)
                .requester(requester)
                .word(word)
                .description(request.getDescription())
                .status(SuggestionStatus.PENDING)
                .build());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularySuggestionResponse> getMine(Long userId) {
        return suggestionRepository.findMineWithDetails(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VocabularySuggestionResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return suggestionRepository.findAllWithDetails(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return suggestionRepository.countByStatus(SuggestionStatus.PENDING);
    }

    @Override
    public void markReviewed(Long id) {
        VocabularySuggestion s = suggestionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOCABULARY_SUGGESTION_NOT_FOUND));
        s.setStatus(SuggestionStatus.REVIEWED);
        s.setReviewedAt(LocalDateTime.now());
        suggestionRepository.save(s);
    }

    private VocabularySuggestionResponse toResponse(VocabularySuggestion s) {
        Category c = s.getCategory();
        User u = s.getRequester();
        return VocabularySuggestionResponse.builder()
                .id(s.getId())
                .word(s.getWord())
                .description(s.getDescription())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .reviewedAt(s.getReviewedAt())
                .categoryId(c != null ? c.getId() : null)
                .categoryName(c != null ? c.getName() : null)
                .requesterId(u != null ? u.getUserId() : null)
                .requesterName(u != null ? u.getFullName() : null)
                .build();
    }
}
