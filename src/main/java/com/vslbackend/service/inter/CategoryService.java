package com.vslbackend.service.inter;

import com.vslbackend.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface CategoryService {
    Page<CategoryResponse> getAllCategories(int page, int size);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(com.vslbackend.dto.request.admin.AdminCreateCategoryRequest request);

    CategoryResponse updateCategory(Long id, com.vslbackend.dto.request.admin.AdminUpdateCategoryRequest request);

    void deleteCategory(Long id);
}
