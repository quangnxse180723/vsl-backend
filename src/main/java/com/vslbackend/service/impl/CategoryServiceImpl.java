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
import com.vslbackend.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final VocabularyRepository vocabularyRepository;
    private final MinioService minioService;

    @Override
    public Page<CategoryResponse> getAllCategories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return categoryRepository.findAll(pageable)
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .imageUrl(category.getImageUrl())
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
                .imageUrl(category.getImageUrl())
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
                .imageUrl(saved.getImageUrl())
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
                .imageUrl(saved.getImageUrl())
                .build();
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (vocabularyRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Khong the xoa danh muc vi van con tu vung nam trong danh muc nay");
        }

        minioService.deleteCategoryImageByUrl(category.getImageUrl());
        categoryRepository.delete(category);
    }

    @Override
    public String uploadCategoryImage(Long id, MultipartFile image) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Replace the previous image (if any) so the bucket doesn't accumulate orphans.
        minioService.deleteCategoryImageByUrl(category.getImageUrl());

        String objectName = minioService.generateCategoryImageObjectName(id, image.getOriginalFilename());
        String publicUrl = minioService.uploadCategoryImage(image, objectName);

        category.setImageUrl(publicUrl);
        categoryRepository.save(category);

        return publicUrl;
    }
}
