package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.CategoryRequestDto;
import com.storechain.group4.dto.CategoryResponseDto;
import com.storechain.group4.dto.CategoryStatsDto;
import com.storechain.group4.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CategoryResponseDto>>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Boolean onlyRoots,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<CategoryResponseDto> categories = categoryService.getCategories(
                keyword, status, parentId, onlyRoots, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", categories));
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

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CategoryStatsDto>> getCategoryStats() {
        CategoryStatsDto stats = categoryService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu thống kê danh mục thành công", stats));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategoryById(@PathVariable Long id) {
        try {
            CategoryResponseDto category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết danh mục thành công", category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(@Valid @RequestBody CategoryRequestDto request) {
        try {
            CategoryResponseDto created = categoryService.createCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Thêm danh mục mới thành công!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto request
    ) {
        try {
            CategoryResponseDto updated = categoryService.updateCategory(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        try {
            CategoryResponseDto updated = categoryService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái danh mục thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công!", "Đã xóa danh mục ID: " + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
