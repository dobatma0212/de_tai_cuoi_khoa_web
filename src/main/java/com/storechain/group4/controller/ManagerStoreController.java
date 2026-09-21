package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.PromotionDto;
import com.storechain.group4.dto.RevenueDto;
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
 * - Nhà vận chuyển (chỉ xem, thêm/sửa/xóa do admin quản lý)
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
            @RequestParam(required = false) Long transporterId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật trạng thái đơn hàng thành công!",
                    managerShopService.updateOrderStatus(currentUser.getId(), id, status,
                            transporterId, reason)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // (Tạm bỏ phần khuyến mãi — sẽ bổ sung lại sau; nghiệp vụ còn giữ trong ManagerShopService)

    // ==================== Nhà vận chuyển (chỉ xem - thêm/sửa/xóa do admin quản lý) ====================

    @GetMapping("/transporters")
    public ResponseEntity<ApiResponse<List<TransporterResponseDto>>> getTransporters() {
        Page<TransporterResponseDto> page = transporterService.getTransporters(
                null, null, 0, 100, "id", "asc");
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách nhà vận chuyển thành công", page.getContent()));
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
