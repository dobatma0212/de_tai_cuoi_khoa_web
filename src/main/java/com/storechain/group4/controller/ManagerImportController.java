package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ImportReceiptDto;
import com.storechain.group4.dto.ImportReceiptRequestDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API nhập hàng dành riêng cho Manager chi nhánh:
 * lập nháp, sửa nháp, xác nhận (cộng tồn kho), hủy phiếu.
 * Chỉ MANAGER được gọi — kể cả admin cũng không ghi được phiếu nhập.
 */
@RestController
@RequestMapping("/api/manager/imports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ManagerImportController {

    private final ImportService importService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ImportReceiptDto>>> getReceipts(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách phiếu nhập thành công",
                    importService.getMyReceipts(currentUser.getId(), status, fromDate, toDate)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImportReceiptDto>> getReceipt(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy chi tiết phiếu nhập thành công",
                    importService.getReceipt(currentUser.getId(), id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ImportReceiptDto>> createReceipt(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ImportReceiptRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Đã lưu phiếu nhập nháp",
                    importService.createReceipt(currentUser.getId(), request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ImportReceiptDto>> updateReceipt(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ImportReceiptRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã cập nhật phiếu nhập",
                    importService.updateReceipt(currentUser.getId(), id, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<ImportReceiptDto>> confirmReceipt(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã xác nhận phiếu nhập — tồn kho chi nhánh đã được cộng",
                    importService.confirmReceipt(currentUser.getId(), id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ImportReceiptDto>> cancelReceipt(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã hủy phiếu nhập",
                    importService.cancelReceipt(currentUser.getId(), id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }
}
