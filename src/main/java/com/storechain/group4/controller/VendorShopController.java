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

import java.util.List;

/**
 * API quản lý trang chủ cửa hàng của vendor:
 * - Danh sách cửa hàng đang hoạt động của mình (để chọn shop cần quản lý)
 * - Xem / cập nhật thông tin cửa hàng (tên, mô tả, logo, banner, SĐT, địa chỉ)
 * - Nếu không truyền shopId thì mặc định thao tác trên shop active mới nhất
 */
@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class VendorShopController {

    private final ShopService shopService;

    @GetMapping("/shops")
    public ResponseEntity<ApiResponse<List<ShopResponseDto>>> getMyActiveShops(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<ShopResponseDto> shops = shopService.getMyActiveShops(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cửa hàng của bạn thành công", shops));
    }

    @GetMapping("/shop")
    public ResponseEntity<ApiResponse<ShopResponseDto>> getMyShop(
            @RequestParam(required = false) Long shopId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShopResponseDto shop = shopService.getMyShop(currentUser.getId(), shopId);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cửa hàng thành công!", shop));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/shop")
    public ResponseEntity<ApiResponse<ShopResponseDto>> updateMyShop(
            @RequestParam(required = false) Long shopId,
            @Valid @RequestBody ShopUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            ShopResponseDto updated = shopService.updateMyShop(request, currentUser.getId(), shopId);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trang chủ cửa hàng thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
