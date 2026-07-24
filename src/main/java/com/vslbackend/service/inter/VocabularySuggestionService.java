package com.vslbackend.service.inter;

import com.vslbackend.dto.request.user.CreateVocabularySuggestionRequest;
import com.vslbackend.dto.response.VocabularySuggestionResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VocabularySuggestionService {

    /** Nguoi dung gui de xuat tu vung moi. Chan neu tu da ton tai (so khop chinh xac, toan he thong). */
    VocabularySuggestionResponse submit(Long userId, CreateVocabularySuggestionRequest request);

    /** Danh sach de xuat cua chinh nguoi dung. */
    List<VocabularySuggestionResponse> getMine(Long userId);

    /** Admin: danh sach tat ca de xuat (moi nhat truoc). */
    Page<VocabularySuggestionResponse> getAll(int page, int size);

    /** Admin: so de xuat dang cho (PENDING). */
    long countPending();

    /** Admin: danh dau da xem. */
    void markReviewed(Long id);
}
