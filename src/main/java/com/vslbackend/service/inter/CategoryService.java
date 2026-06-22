package com.vslbackend.service.inter;

import com.vslbackend.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface CategoryService {
    Page<CategoryResponse> getAllCategories(int page, int size);

    CategoryResponse getCategoryById(Long id);

}
