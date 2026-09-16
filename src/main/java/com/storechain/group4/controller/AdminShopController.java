package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API quản trị duyệt / quản lý cửa hàng (ADMIN, MANAGER):
 * - Danh sách shop phân trang + lọc
 * - Duyệt / từ chối đơn đăng ký mở cửa hàng
 * - Khóa / mở khóa cửa hàng
 */
@RestController
@RequestMapping("/api/admin/shops")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AdminShopController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShopResponseDto>>> getShops(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<ShopResponseDto> shops = shopService.getShops(keyword, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cửa hàng thành công", shops));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponseDto>> getShopById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy chi tiết cửa hàng thành công", shopService.getShopById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ShopResponseDto>> approveShop(@PathVariable Long id) {
        try {
            ShopResponseDto updated = shopService.approveShop(id);
            return ResponseEntity.ok(ApiResponse.success("Duyệt cửa hàng thành công! Chủ shop đã được cấp quyền bán hàng.", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ShopResponseDto>> rejectShop(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            ShopResponseDto updated = shopService.rejectShop(id, reason);
            return ResponseEntity.ok(ApiResponse.success("Đã từ chối đơn đăng ký cửa hàng", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ShopResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            ShopResponseDto updated = shopService.updateShopStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái cửa hàng thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
