package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopUpdateRequest;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API quản lý trang chủ cửa hàng của vendor:
 * - Xem / cập nhật thông tin cửa hàng (tên, mô tả, logo, banner, SĐT, địa chỉ)
 */
@RestController
@RequestMapping("/api/vendor/shop")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class VendorShopController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopResponseDto>> getMyShop(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShopResponseDto shop = shopService.getMyShop(currentUser.getId());
            if (shop == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Bạn chưa có cửa hàng"));
            }
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cửa hàng thành công!", shop));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ShopResponseDto>> updateMyShop(
            @Valid @RequestBody ShopUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShopResponseDto updated = shopService.updateMyShop(request, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trang chủ cửa hàng thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
