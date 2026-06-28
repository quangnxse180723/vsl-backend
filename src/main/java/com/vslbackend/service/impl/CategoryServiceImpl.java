package com.vslbackend.service.impl;

import com.vslbackend.dto.response.CategoryResponse;
import com.vslbackend.entity.Category;
import com.vslbackend.repository.CategoryRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.inter.CategoryService;
import com.vslbackend.dto.request.admin.AdminCreateCategoryRequest;
import com.vslbackend.dto.request.admin.AdminUpdateCategoryRequest;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final VocabularyRepository vocabularyRepository;

    @Override
    public Page<CategoryResponse> getAllCategories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return categoryRepository.findAll(pageable)
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .build());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    @Override
    public CategoryResponse createCategory(AdminCreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ten danh muc da ton tai");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

    @Override
    public CategoryResponse updateCategory(Long id, AdminUpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ten danh muc da ton tai");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (vocabularyRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Khong the xoa danh muc vi van con tu vung nam trong danh muc nay");
        }

        categoryRepository.delete(category);
    }
}
