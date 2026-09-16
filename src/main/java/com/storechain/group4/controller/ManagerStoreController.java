package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.PromotionDto;
import com.storechain.group4.dto.RevenueDto;
import com.storechain.group4.dto.TransporterRequestDto;
import com.storechain.group4.dto.TransporterResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ManagerShopService;
import com.storechain.group4.service.TransporterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API quản trị của chi nhánh dành cho Manager:
 * - Đơn hàng theo trạng thái (mới, đã xác nhận, đã lấy hàng, đang giao, đã giao, hủy, trả hàng)
 * - Chương trình khuyến mãi (giảm % / giảm phí vận chuyển)
 * - Nhà vận chuyển (tên, phí vận chuyển)
 * - Doanh thu chi nhánh
 */
@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ManagerStoreController {

    private final ManagerShopService managerShopService;
    private final TransporterService transporterService;

    // ==================== Đơn hàng ====================

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrders(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách đơn hàng thành công",
                    managerShopService.getOrders(currentUser.getId(), status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật trạng thái đơn hàng thành công!",
                    managerShopService.updateOrderStatus(currentUser.getId(), id, status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Khuyến mãi ====================

    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getPromotions(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách khuyến mãi thành công",
                    managerShopService.getPromotions(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(
            @RequestBody PromotionDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Tạo khuyến mãi thành công!",
                    managerShopService.savePromotion(currentUser.getId(), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            request.setId(id);
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật khuyến mãi thành công!",
                    managerShopService.savePromotion(currentUser.getId(), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/promotions/{id}/active")
    public ResponseEntity<ApiResponse<PromotionDto>> togglePromotion(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            PromotionDto current = managerShopService.getPromotions(currentUser.getId())
                    .stream().filter(p -> p.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi."));
            current.setActive(active);
            return ResponseEntity.ok(ApiResponse.success(
                    active ? "Đã kích hoạt khuyến mãi!" : "Đã tạm dừng khuyến mãi!",
                    managerShopService.savePromotion(currentUser.getId(), current)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<String>> deletePromotion(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            managerShopService.deletePromotion(currentUser.getId(), id);
            return ResponseEntity.ok(ApiResponse.success("Xóa khuyến mãi thành công!", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Nhà vận chuyển ====================

    @GetMapping("/transporters")
    public ResponseEntity<ApiResponse<List<TransporterResponseDto>>> getTransporters() {
        Page<TransporterResponseDto> page = transporterService.getTransporters(
                null, null, 0, 100, "id", "asc");
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách nhà vận chuyển thành công", page.getContent()));
    }

    @PostMapping("/transporters")
    public ResponseEntity<ApiResponse<TransporterResponseDto>> createTransporter(
            @RequestBody TransporterRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Thêm nhà vận chuyển thành công!",
                    transporterService.createTransporter(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/transporters/{id}")
    public ResponseEntity<ApiResponse<TransporterResponseDto>> updateTransporter(
            @PathVariable Long id,
            @RequestBody TransporterRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật nhà vận chuyển thành công!",
                    transporterService.updateTransporter(id, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/transporters/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTransporter(@PathVariable Long id) {
        try {
            transporterService.deleteTransporter(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa nhà vận chuyển thành công!", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Doanh thu ====================

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueDto>> getRevenue(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy doanh thu chi nhánh thành công",
                    managerShopService.getRevenue(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }
}
