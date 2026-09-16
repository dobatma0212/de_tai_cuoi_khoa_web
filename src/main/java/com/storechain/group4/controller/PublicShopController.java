package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopHomeDto;
import com.storechain.group4.service.ManagerShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API công khai cho trang chủ chi nhánh:
 * - Thông tin chi nhánh + danh mục kèm sản phẩm đại diện
 * - Tìm kiếm sản phẩm, gợi ý sản phẩm khi gõ từ khóa
 */
@RestController
@RequestMapping("/api/public/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicShopController {

    private final ManagerShopService managerShopService;

    @GetMapping("/{id}/home")
    public ResponseEntity<ApiResponse<ShopHomeDto>> getShopHome(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy trang chủ chi nhánh thành công",
                    managerShopService.getShopHome(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<com.storechain.group4.dto.ProductPageDto>> searchProducts(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tìm kiếm sản phẩm thành công",
                managerShopService.searchShopProducts(id, keyword, categoryId,
                        minPrice, maxPrice, size, sort, page, Math.min(pageSize, 50))));
    }

    @GetMapping("/{id}/suggest")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> suggestProducts(
            @PathVariable Long id,
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                "Gợi ý sản phẩm",
                managerShopService.suggestShopProducts(id, keyword)));
    }
}
