package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ShopRequestDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopStatsDto;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shops")
@PreAuthorize("hasRole('ADMIN')")
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
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chi nhánh thành công", shops));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ShopStatsDto>> getShopStats() {
        ShopStatsDto stats = shopService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu thống kê chi nhánh thành công", stats));
    }

    @GetMapping("/managers")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAvailableManagers() {
        List<UserResponseDto> managers = shopService.getAvailableManagers();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài khoản Quản lý thành công", managers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponseDto>> getShopById(@PathVariable Long id) {
        try {
            ShopResponseDto shop = shopService.getShopById(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi nhánh thành công", shop));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShopResponseDto>> createShop(@Valid @RequestBody ShopRequestDto request) {
        try {
            ShopResponseDto created = shopService.createShop(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tạo chi nhánh mới thành công!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponseDto>> updateShop(
            @PathVariable Long id,
            @Valid @RequestBody ShopRequestDto request
    ) {
        try {
            ShopResponseDto updated = shopService.updateShop(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin chi nhánh thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ShopResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        try {
            ShopResponseDto updated = shopService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái chi nhánh thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/manager")
    public ResponseEntity<ApiResponse<ShopResponseDto>> assignManager(
            @PathVariable Long id,
            @RequestParam(required = false) Long managerId
    ) {
        try {
            ShopResponseDto updated = shopService.assignManager(id, managerId);
            String message = (managerId == null || managerId <= 0)
                    ? "Đã gỡ bỏ Quản lý phụ trách chi nhánh!"
                    : "Bổ nhiệm Quản lý chi nhánh thành công!";
            return ResponseEntity.ok(ApiResponse.success(message, updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteShop(@PathVariable Long id) {
        try {
            shopService.deleteShop(id);
            return ResponseEntity.ok(ApiResponse.success(
                    "Xóa chi nhánh thành công!",
                    "Đã xóa chi nhánh ID: " + id
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
