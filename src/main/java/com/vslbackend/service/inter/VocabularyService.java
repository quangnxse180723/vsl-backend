package com.vslbackend.service.inter;

import com.vslbackend.dto.request.CreateVocabularyRequest;
import com.vslbackend.dto.response.VocabularyResponse;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface VocabularyService {

    Page<VocabularyResponse> getAll(int page, int size);

    VocabularyResponse getById(@NonNull Long id);

    Page<VocabularyResponse> search(@NonNull String keyword, int page, int size);

    Page<VocabularyResponse> getByCategory(@NonNull Long categoryId, int page, int size);

    VocabularyResponse create(CreateVocabularyRequest request);

    VocabularyResponse update(Long id, CreateVocabularyRequest request);

    String uploadTutorialVideo(Long vocabularyId, MultipartFile video, int expectedId);

    String uploadVocabularyImage(Long vocabularyId, MultipartFile image);

    void delete(Long id);
}
