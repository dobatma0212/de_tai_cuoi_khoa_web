package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ReviewDto;
import com.storechain.group4.service.StorefrontCatalogService;
import com.storechain.group4.service.StorefrontService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API công khai cho người mua (không cần đăng nhập):
 * danh sách sản phẩm, chi tiết sản phẩm và đánh giá sản phẩm.
 */
@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicProductController {

    private final StorefrontCatalogService catalogService;
    private final StorefrontService storefrontService;

    /** Danh sách sản phẩm đang bán (lọc theo categoryId / keyword nếu có) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách sản phẩm thành công",
                catalogService.listProducts(categoryId, keyword)));
    }

    /** Sản phẩm nổi bật hiển thị ở trang chủ */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> featuredProducts() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy sản phẩm nổi bật thành công",
                catalogService.listRepresentativeProducts()));
    }

    /** Chi tiết sản phẩm kèm điểm đánh giá trung bình */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProduct(@PathVariable Long id) {
        try {
            ProductResponseDto dto = storefrontService.getProductDetail(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Đánh giá của sản phẩm (công khai) */
    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy đánh giá sản phẩm thành công",
                storefrontService.getProductReviews(id)));
    }
}
