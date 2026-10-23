package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductPageDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API công khai cho trang chủ HÃNG (mua sắm chung, không theo chi nhánh):
 * - Danh sách sản phẩm toàn hãng: tìm kiếm, lọc danh mục/giá/size, phân trang
 * - Gợi ý sản phẩm khi gõ từ khóa
 * Sau khi đặt hàng, đơn được hệ thống phân phối về chi nhánh gần địa chỉ nhận nhất.
 */
@RestController
@RequestMapping("/api/public/catalog")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicCatalogController {

    private final CatalogService catalogService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductPageDto>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tìm kiếm sản phẩm thành công",
                catalogService.searchProducts(keyword, categoryId, minPrice, maxPrice,
                        size, color, sort, page, Math.min(pageSize, 50))));
    }

    /** Top 20 sản phẩm trang chủ: type = new | bestseller | toprated | favorite, phân trang trong 20 sản phẩm */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<ProductPageDto>> topProducts(
            @RequestParam(defaultValue = "new") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int pageSize) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách sản phẩm thành công",
                catalogService.topProducts(type, page, pageSize)));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> suggestProducts(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                "Gợi ý sản phẩm",
                catalogService.suggestProducts(keyword)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<com.storechain.group4.dto.ShopCategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh mục sản phẩm toàn hãng thành công",
                catalogService.getCategories()));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProduct(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy chi tiết sản phẩm thành công",
                    catalogService.getProduct(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }
}
