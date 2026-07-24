package com.vslbackend.service.inter;

import com.vslbackend.dto.response.VocabularyExistsResponse;
import com.vslbackend.dto.request.CreateVocabularyRequest;
import com.vslbackend.dto.response.VocabularyResponse;
import com.vslbackend.dto.response.VocabularySynonymResponse;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface VocabularyService {

    Page<VocabularyResponse> getAll(int page, int size);

    VocabularyResponse getById(@NonNull Long id);

    Page<VocabularyResponse> search(@NonNull String keyword, int page, int size);

    Page<VocabularyResponse> getByCategory(@NonNull Long categoryId, int page, int size);

    /** Kiem tra tu vung da ton tai chua (so khop chinh xac, khong phan biet hoa/thuong, toan he thong). */
    VocabularyExistsResponse checkWordExists(@NonNull String word);

    /** Dung AI quet cac tu da co co nghia giong / dong nghia voi tu ung vien (goi y, khong chan). */
    VocabularySynonymResponse findSynonyms(@NonNull String word);
    VocabularyResponse create(CreateVocabularyRequest request);

    VocabularyResponse update(Long id, CreateVocabularyRequest request);

    String uploadTutorialVideo(Long vocabularyId, MultipartFile video, int expectedId);

    String uploadVocabularyImage(Long vocabularyId, MultipartFile image);

    void delete(Long id);
}
