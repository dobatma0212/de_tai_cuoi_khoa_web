package com.storechain.group4.service;

import com.storechain.group4.dto.CategoryRequestDto;
import com.storechain.group4.dto.CategoryResponseDto;
import com.storechain.group4.dto.CategoryStatsDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CategoryService {

    Page<CategoryResponseDto> getCategories(
            String keyword,
            String status,
            Long parentId,
            Boolean onlyRoots,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    List<CategoryResponseDto> getAllCategoriesTree();

    List<CategoryResponseDto> getRootCategories();

    List<CategoryResponseDto> getSubCategories(Long parentId);

    List<CategoryResponseDto> getAllActiveCategoriesFlat();

    CategoryResponseDto getCategoryById(Long id);

    CategoryResponseDto createCategory(CategoryRequestDto request);

    CategoryResponseDto updateCategory(Long id, CategoryRequestDto request);

    CategoryResponseDto updateStatus(Long id, String status);

    void deleteCategory(Long id);

    CategoryStatsDto getStats();
}
