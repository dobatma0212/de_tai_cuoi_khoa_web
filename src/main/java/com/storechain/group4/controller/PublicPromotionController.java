package com.storechain.group4.controller;

import com.storechain.group4.dto.*;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class PublicPromotionController {

    private final PromotionService promotionService;

    // GET /flash-sales - public, no auth needed
    // Returns list of active flash sales
    @GetMapping("/flash-sales")
    public ResponseEntity<ApiResponse<List<PromotionResponseDto>>> getActiveFlashSales() {
        List<PromotionResponseDto> flashSales = promotionService.getActiveFlashSales();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Flash Sale thành công", flashSales));
    }

    // POST /check-voucher - authenticated user
    // Preview voucher validation without applying
    @PostMapping("/check-voucher")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApplyVoucherResponseDto>> checkVoucher(
            @Valid @RequestBody ApplyVoucherRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            ApplyVoucherResponseDto result = promotionService.checkVoucher(
                    request.getCode(), request.getOrderTotal(), currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // POST /apply-voucher - authenticated user
    // Apply voucher (increments count, creates usage record)
    @PostMapping("/apply-voucher")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApplyVoucherResponseDto>> applyVoucher(
            @Valid @RequestBody ApplyVoucherRequestDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            ApplyVoucherResponseDto result = promotionService.applyVoucher(
                    request.getCode(), request.getOrderTotal(), currentUser.getId());
            if (!result.isValid()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage()));
            }
            return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
