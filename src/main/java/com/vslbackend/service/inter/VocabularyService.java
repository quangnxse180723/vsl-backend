package com.vslbackend.service.inter;

import com.vslbackend.dto.response.VocabularyResponse;
import lombok.NonNull;
import org.springframework.data.domain.Page;

public interface VocabularyService {

    Page<VocabularyResponse> getAll(int page, int size);

    VocabularyResponse getById(@NonNull Long id);

    Page<VocabularyResponse> search(@NonNull String keyword, int page, int size);

    Page<VocabularyResponse> getByCategory(@NonNull Long categoryId, int page, int size);
}
