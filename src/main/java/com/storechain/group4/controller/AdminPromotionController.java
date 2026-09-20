package com.storechain.group4.controller;

import com.storechain.group4.dto.*;
import com.storechain.group4.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promotions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PromotionResponseDto>>> getPromotions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<PromotionResponseDto> promotions = promotionService.getPromotions(
                keyword, status, type, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khuyến mãi thành công", promotions));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PromotionStatsDto>> getPromotionStats() {
        PromotionStatsDto stats = promotionService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu thống kê khuyến mãi thành công", stats));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<java.util.List<ProductOptionDto>>> getProductOptions() {
        java.util.List<ProductOptionDto> products = promotionService.getProductOptions();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponseDto>> getPromotionById(@PathVariable Long id) {
        try {
            PromotionResponseDto promotion = promotionService.getPromotionById(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết khuyến mãi thành công", promotion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponseDto>> createPromotion(
            @Valid @RequestBody PromotionRequestDto request
    ) {
        try {
            PromotionResponseDto created = promotionService.createPromotion(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Thêm khuyến mãi mới thành công!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponseDto>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequestDto request
    ) {
        try {
            PromotionResponseDto updated = promotionService.updatePromotion(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật khuyến mãi thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PromotionResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        try {
            PromotionResponseDto updated = promotionService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái khuyến mãi thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePromotion(@PathVariable Long id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok(ApiResponse.success(
                    "Xóa khuyến mãi thành công!",
                    "Đã xóa khuyến mãi ID: " + id
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
