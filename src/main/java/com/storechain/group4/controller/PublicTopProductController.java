package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.service.UserEngagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API công khai: top 20 sản phẩm mới / bán chạy / đánh giá cao / yêu thích nhiều.
 * Hỗ trợ phân trang (mặc định size=20).
 */
@RestController
@RequestMapping("/api/public/top")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicTopProductController {

    private final UserEngagementService engagementService;

    /**
     * @param type newest | bestseller | top_rated | most_favorited
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getTopProducts(
            @RequestParam(defaultValue = "newest") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Top sản phẩm: " + type,
                engagementService.getTopProducts(type, page, Math.min(size, 20))));
    }
}
