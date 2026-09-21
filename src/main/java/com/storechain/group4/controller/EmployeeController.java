package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.EmployeeStatsDto;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * API dành cho nhân viên chi nhánh (ROLE_EMPLOYEE):
 * - Quản lý sản phẩm của chi nhánh mình được phân công (thêm / sửa / xóa)
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

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ShopCategoryDto>>> getMyCategories(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách danh mục thành công",
                    employeeService.getMyCategories(currentUser.getId())));
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

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Thêm sản phẩm mới thành công!",
                    employeeService.createProduct(currentUser.getId(), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật sản phẩm thành công!",
                    employeeService.updateProduct(currentUser.getId(), id, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            employeeService.deleteProduct(currentUser.getId(), id);
            return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công!",
                    "Đã xóa sản phẩm ID: " + id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/products/{id}/representative")
    public ResponseEntity<ApiResponse<ProductResponseDto>> setRepresentative(
            @PathVariable Long id,
            @RequestParam boolean representative,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            String msg = representative
                    ? "Đã đặt làm sản phẩm đại diện của danh mục!"
                    : "Đã bỏ chọn sản phẩm đại diện.";
            return ResponseEntity.ok(ApiResponse.success(msg,
                    employeeService.setRepresentative(currentUser.getId(), id, representative)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            String url = employeeService.uploadImage(file);
            return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công!",
                    Map.of("url", url)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
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
     * Xác nhận đơn (CONFIRMED kèm transporterId — đơn được gắn cho nhân viên để tính hoa hồng),
     * hủy đơn (CANCELLED kèm lý do, hoàn tồn kho), trả hàng (RETURNED).
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
