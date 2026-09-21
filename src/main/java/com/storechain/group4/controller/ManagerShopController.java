package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ManagerShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * API quản lý chi nhánh dành cho Manager:
 * - Xem thông tin chi nhánh do admin bổ nhiệm (chỉ đọc, chỉ admin được sửa thông tin chi nhánh)
 * - Quản lý sản phẩm theo danh mục (Bộ áo quần / Phụ kiện), đặt sản phẩm đại diện
 * - Tải ảnh sản phẩm từ máy lên
 */
@RestController
@RequestMapping("/api/manager/shop")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ManagerShopController {

    private final ManagerShopService managerShopService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopResponseDto>> getMyShop(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy thông tin chi nhánh thành công",
                    managerShopService.getMyShop(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Danh mục sản phẩm của chi nhánh ====================

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ShopCategoryDto>>> getMyCategories(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách danh mục thành công",
                    managerShopService.getMyCategories(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<ShopCategoryDto>> addCategory(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Thêm danh mục mới thành công!",
                    managerShopService.addCategory(currentUser.getId(),
                            body.getOrDefault("name", ""))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<ShopCategoryDto>> renameCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Đổi tên danh mục thành công!",
                    managerShopService.renameCategory(currentUser.getId(), id,
                            body.getOrDefault("name", ""))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            managerShopService.deleteCategory(currentUser.getId(), id);
            return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công!",
                    "Đã xóa danh mục ID: " + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getMyProducts(
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách sản phẩm thành công",
                    managerShopService.getMyProducts(currentUser.getId(), categoryId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Thêm sản phẩm mới thành công!",
                    managerShopService.createProduct(currentUser.getId(), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật sản phẩm thành công!",
                    managerShopService.updateProduct(currentUser.getId(), id, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            managerShopService.deleteProduct(currentUser.getId(), id);
            return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công!",
                    "Đã xóa sản phẩm ID: " + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/products/{id}/representative")
    public ResponseEntity<ApiResponse<ProductResponseDto>> setRepresentative(
            @PathVariable Long id,
            @RequestParam boolean representative,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            String msg = representative
                    ? "Đã đặt làm sản phẩm đại diện của danh mục!"
                    : "Đã bỏ chọn sản phẩm đại diện.";
            return ResponseEntity.ok(ApiResponse.success(msg,
                    managerShopService.setRepresentative(currentUser.getId(), id, representative)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            String url = managerShopService.uploadImage(file);
            return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công!",
                    Map.of("url", url)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
