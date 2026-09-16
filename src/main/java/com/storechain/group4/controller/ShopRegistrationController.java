package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ShopRegistrationRequest;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API đăng ký mở cửa hàng (trong trang Profile):
 * - Xem trạng thái đơn đăng ký shop của người dùng hiện tại
 * - Gửi đơn đăng ký mở cửa hàng (chờ admin duyệt)
 */
@RestController
@RequestMapping("/api/profile/shop")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class ShopRegistrationController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopResponseDto>> getMyShop(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ShopResponseDto shop = shopService.getMyShop(currentUser.getId());
        String message = shop != null
                ? "Lấy thông tin cửa hàng thành công!"
                : "Bạn chưa đăng ký mở cửa hàng";
        return ResponseEntity.ok(ApiResponse.success(message, shop));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ShopResponseDto>> registerShop(
            @Valid @RequestBody ShopRegistrationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShopResponseDto created = shopService.registerShop(request, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Gửi đăng ký mở cửa hàng thành công! Vui lòng chờ quản trị viên duyệt.", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
