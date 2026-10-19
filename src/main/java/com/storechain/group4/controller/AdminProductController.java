package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ProductStatsDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<ProductResponseDto> products = productService.getProducts(
                keyword, shopId, categoryId, status, inStock, minPrice, maxPrice, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ProductStatsDto>> getProductStats() {
        ProductStatsDto stats = productService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê sản phẩm thành công", stats));
    }

    @GetMapping("/shops")
    public ResponseEntity<ApiResponse<List<ShopResponseDto>>> getAvailableShops() {
        List<ShopResponseDto> shops = productService.getAvailableShops();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chi nhánh thành công", shops));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ShopCategoryDto>>> getCategoriesByShop(
            @RequestParam(required = false) Long shopId
    ) {
        List<ShopCategoryDto> categories = productService.getCategoriesByShop(shopId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục sản phẩm thành công", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        try {
            ProductResponseDto product = productService.getProductById(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", product));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody ProductRequestDto request) {
        try {
            ProductResponseDto created = productService.createProduct(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Thêm sản phẩm mới thành công!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request
    ) {
        try {
            ProductResponseDto updated = productService.updateProduct(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin sản phẩm thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        try {
            ProductResponseDto updated = productService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái sản phẩm thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateStock(
            @PathVariable Long id,
            @RequestParam int stock
    ) {
        try {
            ProductResponseDto updated = productService.updateStock(id, stock);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật tồn kho sản phẩm thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/representative")
    public ResponseEntity<ApiResponse<ProductResponseDto>> setRepresentative(
            @PathVariable Long id,
            @RequestParam boolean representative
    ) {
        try {
            ProductResponseDto updated = productService.setRepresentative(id, representative);
            return ResponseEntity.ok(ApiResponse.success(
                    representative ? "Đã đặt làm sản phẩm tiêu biểu!" : "Đã hủy sản phẩm tiêu biểu!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/badge")
    public ResponseEntity<ApiResponse<ProductResponseDto>> toggleBadge(
            @PathVariable Long id,
            @RequestParam String badge,
            @RequestParam boolean value
    ) {
        try {
            ProductResponseDto updated = productService.toggleBadge(id, badge, value);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật nhãn sản phẩm thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công!", "Đã xóa sản phẩm ID: " + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = productService.uploadImage(file);
            return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công!", Map.of("url", imageUrl)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Không thể tải ảnh lên: " + e.getMessage()));
        }
    }
}
