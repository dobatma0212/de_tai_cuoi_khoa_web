package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ManagerShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API quản lý chi nhánh dành cho Manager:
 * - Xem thông tin chi nhánh do admin bổ nhiệm (chỉ đọc, chỉ admin được sửa thông tin chi nhánh)
 * - Xem sản phẩm theo danh mục (chỉ đọc — thêm/sửa/xóa sản phẩm do nhân viên thực hiện)
 * - Xem tồn kho của từng sản phẩm tại chi nhánh
 */
@RestController
@RequestMapping("/api/manager/shop")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ManagerShopController {

    private final ManagerShopService managerShopService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopResponseDto>> getMyShop(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy thông tin chi nhánh thành công",
                    managerShopService.getMyShop(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Danh mục sản phẩm của chi nhánh ====================

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ShopCategoryDto>>> getMyCategories(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách danh mục thành công",
                    managerShopService.getMyCategories(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    // Manager chỉ được xem danh mục; thêm/sửa/xóa danh mục do admin và nhân viên thực hiện

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getMyProducts(
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách sản phẩm thành công",
                    managerShopService.getMyProducts(currentUser.getId(), categoryId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    // Manager chỉ được xem sản phẩm; thêm/sửa/xóa sản phẩm do nhân viên thực hiện

    // ==================== Quản lý nhân viên chi nhánh ====================

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<com.storechain.group4.dto.EmployeeDto>>> getMyEmployees(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách nhân viên thành công",
                    managerShopService.getMyEmployees(currentUser.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Bổ nhiệm nhân viên: tạo tài khoản mới hoặc thăng USER hiện có thành nhân viên chi nhánh */
    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<com.storechain.group4.dto.EmployeeDto>> hireEmployee(
            @RequestBody com.storechain.group4.dto.HireEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Bổ nhiệm nhân viên thành công!",
                    managerShopService.hireEmployee(currentUser.getId(), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Xem thống kê của một nhân viên theo tháng (số công, đơn đã bán, doanh số,
     * hoa hồng) — dùng được cả cho nhân viên đang làm việc và đã sa thải.
     */
    @GetMapping("/employees/{id}/stats")
    public ResponseEntity<ApiResponse<com.storechain.group4.dto.EmployeeStatsDto>> getEmployeeStats(
            @PathVariable Long id,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy thống kê nhân viên thành công",
                    managerShopService.getEmployeeStats(currentUser.getId(), id, month)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Sa thải nhân viên: trả về quyền khách hàng (không xóa tài khoản, giữ lịch sử để tra cứu) */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<String>> fireEmployee(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            managerShopService.fireEmployee(currentUser.getId(), id);
            return ResponseEntity.ok(ApiResponse.success("Đã sa thải nhân viên.",
                    "Nhân viên ID: " + id + " đã được gỡ khỏi chi nhánh."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
