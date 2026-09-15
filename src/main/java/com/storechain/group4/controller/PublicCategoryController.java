package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.CategoryResponseDto;
import com.storechain.group4.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllActiveCategories() {
        List<CategoryResponseDto> categories = categoryService.getAllActiveCategoriesFlat();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục hoạt động thành công", categories));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategoryTree() {
        List<CategoryResponseDto> tree = categoryService.getAllCategoriesTree();
        return ResponseEntity.ok(ApiResponse.success("Lấy cây danh mục thành công", tree));
    }

    @GetMapping("/roots")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getRootCategories() {
        List<CategoryResponseDto> roots = categoryService.getRootCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục gốc thành công", roots));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategoryById(@PathVariable Long id) {
        try {
            CategoryResponseDto category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin danh mục thành công", category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/sub/{parentId}")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getSubCategories(@PathVariable Long parentId) {
        List<CategoryResponseDto> subs = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục con thành công", subs));
    }
}
