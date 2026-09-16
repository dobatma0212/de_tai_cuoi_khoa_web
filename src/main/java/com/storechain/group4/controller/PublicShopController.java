package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.service.ProductService;
import com.storechain.group4.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API công khai xem trang chủ cửa hàng:
 * - Danh sách cửa hàng đang hoạt động
 * - Chi tiết cửa hàng + danh sách sản phẩm đang bán
 */
@RestController
@RequestMapping("/api/public/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class PublicShopController {

    private final ShopService shopService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShopResponseDto>>> getActiveShops(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Page<ShopResponseDto> shops = shopService.getActiveShops(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cửa hàng thành công", shops));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponseDto>> getShop(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy thông tin cửa hàng thành công", shopService.getPublicShop(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getShopProducts(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        try {
            Page<ProductResponseDto> products = productService.getPublicProducts(id, keyword, page, size);
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm của cửa hàng thành công", products));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }
}
