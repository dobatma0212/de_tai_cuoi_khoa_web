package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.FavoriteDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ProductViewDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.UserEngagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API tương tác người dùng: yêu thích, đã xem, top sản phẩm trang chủ.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class UserEngagementController {

    private final UserEngagementService engagementService;

    // ==================== Yêu thích ====================

    /** Toggle like/unlike sản phẩm */
    @PostMapping("/favorites/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFavorite(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            boolean favorited = engagementService.toggleFavorite(currentUser.getId(), productId);
            return ResponseEntity.ok(ApiResponse.success(
                    favorited ? "Đã thêm vào yêu thích!" : "Đã bỏ yêu thích!",
                    Map.of("favorited", favorited, "productId", productId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/favorites/{productId}/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFavorite(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        boolean favorited = engagementService.isFavorited(currentUser.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of("favorited", favorited)));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<Page<FavoriteDto>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Danh sách yêu thích",
                engagementService.getMyFavorites(currentUser.getId(), page, size)));
    }

    @GetMapping("/favorites/count")
    public ResponseEntity<ApiResponse<Long>> countFavorites(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Số sản phẩm yêu thích",
                engagementService.countMyFavorites(currentUser.getId())));
    }

    // ==================== Đã xem ====================

    @PostMapping("/views/{productId}")
    public ResponseEntity<ApiResponse<String>> recordView(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            engagementService.recordProductView(currentUser.getId(), productId);
            return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận lượt xem", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/views")
    public ResponseEntity<ApiResponse<Page<ProductViewDto>>> getMyViewed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sản phẩm đã xem",
                engagementService.getMyViewedProducts(currentUser.getId(), page, size)));
    }

    // ==================== Top sản phẩm (cũng có bản public) ====================

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getTopProducts(
            @RequestParam(defaultValue = "newest") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Top sản phẩm (" + type + ")",
                engagementService.getTopProducts(type, page, size)));
    }
}
