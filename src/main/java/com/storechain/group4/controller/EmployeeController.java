package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.EmployeeStatsDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API dành cho nhân viên chi nhánh (ROLE_EMPLOYEE):
 * - Xem sản phẩm chi nhánh (chỉ đọc) để bán tại quầy
 * - Xem hoa hồng theo đơn hàng đã bán được trong tháng
 * - Chấm công hằng ngày / xem số công trong tháng
 * Nhân viên vẫn giữ quyền cơ bản của user (mua sắm, giỏ hàng, đánh giá...).
 */
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmployeeController {

    private final EmployeeService employeeService;
    private final com.storechain.group4.service.TransporterService transporterService;

    /** Danh sách đơn vị vận chuyển (chỉ xem) để chọn khi xác nhận đơn */
    @GetMapping("/transporters")
    public ResponseEntity<ApiResponse<List<com.storechain.group4.dto.TransporterResponseDto>>> getTransporters() {
        var page = transporterService.getTransporters(null, null, 0, 100, "id", "asc");
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách nhà vận chuyển thành công", page.getContent()));
    }

    // ==================== Chi nhánh của nhân viên ====================

    @GetMapping("/shop")
    public ResponseEntity<ApiResponse<ShopResponseDto>> getMyShop(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy thông tin chi nhánh thành công",
                    employeeService.getMyShop(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Sản phẩm của chi nhánh ====================

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getMyProducts(
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách sản phẩm thành công",
                    employeeService.getMyProducts(currentUser.getId(), categoryId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Đơn hàng của chi nhánh (như manager) ====================

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<com.storechain.group4.dto.OrderDto>>> getMyOrders(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách đơn hàng thành công",
                    employeeService.getMyOrders(currentUser.getId(), status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Tạo đơn hàng offline (bán trực tiếp tại quầy): nhân viên chọn sản phẩm của
     * chi nhánh + số lượng + size, hệ thống tự lấy giá, trừ tồn kho, hoàn tất đơn
     * và gắn handledBy để tính hoa hồng. Trả về đơn để in hóa đơn.
     */
    @PostMapping("/orders/offline")
    public ResponseEntity<ApiResponse<com.storechain.group4.dto.OrderDto>> createOfflineOrder(
            @RequestBody com.storechain.group4.dto.OfflineOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Tạo đơn bán tại quầy thành công!",
                    employeeService.createOfflineOrder(currentUser.getId(), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Xác nhận đơn (CONFIRMED — dùng đơn vị vận chuyển khách đã chọn khi đặt; đơn được gắn
     * cho nhân viên để tính hoa hồng), hủy đơn (CANCELLED kèm lý do, hoàn tồn kho), trả hàng (RETURNED).
     */
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<com.storechain.group4.dto.OrderDto>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) Long transporterId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật trạng thái đơn hàng thành công!",
                    employeeService.updateOrderStatus(currentUser.getId(), id, status,
                            transporterId, reason)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Chấm công & hoa hồng ====================

    @PostMapping("/attendance/check-in")
    public ResponseEntity<ApiResponse<EmployeeStatsDto>> checkIn(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Chấm công thành công! Chúc bạn một ngày làm việc hiệu quả.",
                    employeeService.checkIn(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/commission")
    public ResponseEntity<ApiResponse<EmployeeStatsDto>> getMyCommission(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy thống kê hoa hồng thành công",
                    employeeService.getMyStats(currentUser.getId(), month)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
