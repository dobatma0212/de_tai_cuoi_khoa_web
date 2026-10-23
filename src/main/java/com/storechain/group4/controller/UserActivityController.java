package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ShopperActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Yêu thích, sản phẩm đã xem và quyền đánh giá sản phẩm của người mua (đã đăng nhập).
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserActivityController {

    private final ShopperActivityService activityService;
    private final OrderItemRepository orderItemRepository;
    private final ProductReviewRepository reviewRepository;

    // ==================== Yêu thích ====================

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getFavorites(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm yêu thích thành công",
                activityService.getFavorites(currentUser.getId())));
    }

    /** Chỉ trả danh sách ID để các trang danh sách tô màu trái tim nhanh */
    @GetMapping("/favorites/ids")
    public ResponseEntity<ApiResponse<List<Long>>> getFavoriteIds(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success("OK", activityService.getFavoriteIds(currentUser.getId())));
    }

    @PostMapping("/favorites/{productId}")
    public ResponseEntity<ApiResponse<String>> addFavorite(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            activityService.addFavorite(currentUser.getId(), productId);
            return ResponseEntity.ok(ApiResponse.success("Đã thêm vào danh sách yêu thích", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/favorites/{productId}")
    public ResponseEntity<ApiResponse<String>> removeFavorite(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        activityService.removeFavorite(currentUser.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ khỏi danh sách yêu thích", "OK"));
    }

    // ==================== Sản phẩm đã xem ====================

    @PostMapping("/recently-viewed/{productId}")
    public ResponseEntity<ApiResponse<String>> recordView(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            activityService.recordView(currentUser.getId(), productId);
            return ResponseEntity.ok(ApiResponse.success("OK", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/recently-viewed")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getRecentlyViewed(
            @RequestParam(defaultValue = "12") int limit,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm đã xem thành công",
                activityService.getRecentlyViewed(currentUser.getId(), limit)));
    }

    @DeleteMapping("/recently-viewed")
    public ResponseEntity<ApiResponse<String>> clearRecentlyViewed(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        activityService.clearViewHistory(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa lịch sử đã xem", "OK"));
    }

    // ==================== Quyền đánh giá ====================

    /** Cho giao diện biết có hiện form đánh giá không (chỉ khi đã mua + nhận hàng, và chưa đánh giá) */
    @GetMapping("/products/{id}/review-eligibility")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reviewEligibility(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        boolean purchased = orderItemRepository.countPurchasedByUser(currentUser.getId(), id) > 0;
        boolean reviewed = reviewRepository.existsByProductIdAndUserId(id, currentUser.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("canReview", purchased && !reviewed);
        data.put("purchased", purchased);
        data.put("alreadyReviewed", reviewed);
        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }
}
