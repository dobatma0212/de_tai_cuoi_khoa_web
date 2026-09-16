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

import java.util.List;

/**
 * API đăng ký mở cửa hàng (trong trang Profile):
 * - Xem danh sách cửa hàng & trạng thái các đơn đăng ký của người dùng hiện tại
 * - Gửi đơn đăng ký mở cửa hàng (chờ admin duyệt; mỗi user chỉ 1 đơn chờ duyệt tại một thời điểm)
 */
@RestController
@RequestMapping("/api/profile/shop")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class ShopRegistrationController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopResponseDto>>> getMyShops(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<ShopResponseDto> shops = shopService.getMyShops(currentUser.getId());
        String message = shops.isEmpty()
                ? "Bạn chưa đăng ký mở cửa hàng"
                : "Lấy danh sách cửa hàng thành công!";
        return ResponseEntity.ok(ApiResponse.success(message, shops));
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
