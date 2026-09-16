package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API quản lý sản phẩm của shop (vendor):
 * - CRUD sản phẩm + ẩn/hiện, phân trang, tìm kiếm
 */
@RestController
@RequestMapping("/api/vendor/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class VendorProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        try {
            Page<ProductResponseDto> products = productService.getMyProducts(
                    currentUser.getId(), keyword, status, categoryId, page, size, sortBy, sortDir);
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getMyProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy chi tiết sản phẩm thành công", productService.getMyProductById(currentUser.getId(), id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ProductResponseDto created = productService.createMyProduct(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Thêm sản phẩm mới thành công!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ProductResponseDto updated = productService.updateMyProduct(currentUser.getId(), id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ProductResponseDto updated = productService.updateMyProductStatus(currentUser.getId(), id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái sản phẩm thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            productService.deleteMyProduct(currentUser.getId(), id);
            return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công!", "Đã xóa sản phẩm ID: " + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
