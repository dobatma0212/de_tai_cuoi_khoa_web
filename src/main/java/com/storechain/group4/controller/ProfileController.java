package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ChangePasswordRequest;
import com.storechain.group4.dto.ShippingAddressRequest;
import com.storechain.group4.dto.ShippingAddressResponse;
import com.storechain.group4.dto.UpdateProfileRequest;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quản lý thông tin cá nhân của người dùng đang đăng nhập:
 * - Xem / cập nhật hồ sơ cá nhân
 * - Đổi mật khẩu
 * - CRUD nhiều địa chỉ nhận hàng + đặt địa chỉ mặc định
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    // ==================== Hồ sơ cá nhân ====================

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UserResponseDto profile = profileService.getMyProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công!", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            UserResponseDto updated = profileService.updateMyProfile(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Đổi mật khẩu ====================

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<String>> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            profileService.changeMyPassword(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công!", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Địa chỉ nhận hàng ====================

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<ShippingAddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<ShippingAddressResponse> addresses = profileService.getMyAddresses(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa chỉ nhận hàng thành công!", addresses));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> addMyAddress(
            @Valid @RequestBody ShippingAddressRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShippingAddressResponse created = profileService.addMyAddress(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Thêm địa chỉ nhận hàng thành công!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> updateMyAddress(
            @PathVariable Long id,
            @Valid @RequestBody ShippingAddressRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShippingAddressResponse updated = profileService.updateMyAddress(id, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ nhận hàng thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<ApiResponse<ShippingAddressResponse>> setDefaultAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShippingAddressResponse updated = profileService.setDefaultMyAddress(id, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Đặt địa chỉ mặc định thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMyAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            profileService.deleteMyAddress(id, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ nhận hàng thành công!", "Đã xóa ID: " + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
