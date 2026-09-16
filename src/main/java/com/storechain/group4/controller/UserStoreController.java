package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.OrderCreateRequest;
import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ReviewDto;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.StorefrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * API phía người mua (đã đăng nhập — gồm cả User, Manager, Admin):
 * đánh giá sản phẩm, tải ảnh/video minh họa, giỏ hàng, đặt hàng.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class UserStoreController {

    private final StorefrontService storefrontService;
    private final com.storechain.group4.repository.TransporterRepository transporterRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Danh sách nhà vận chuyển đang hoạt động để khách chọn khi đặt hàng */
    @GetMapping("/transporters")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveTransporters() {
        List<Map<String, Object>> data = transporterRepository.findAll().stream()
                .filter(t -> "active".equalsIgnoreCase(t.getStatus()))
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "name", t.getName(),
                        "phone", t.getPhone() == null ? "" : t.getPhone(),
                        "baseFee", t.getBaseFee()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách nhà vận chuyển thành công", data));
    }

    // ==================== Đánh giá sản phẩm ====================

    @PostMapping("/products/{id}/reviews")
    public ResponseEntity<ApiResponse<ReviewDto>> addReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            int rating = body.get("rating") instanceof Number n ? n.intValue() : 0;
            String comment = body.get("comment") == null ? "" : String.valueOf(body.get("comment"));
            List<ReviewDto.MediaDto> media = null;
            if (body.get("media") instanceof List<?> list) {
                media = list.stream()
                        .filter(m -> m instanceof Map<?, ?>)
                        .map(m -> {
                            Map<?, ?> mm = (Map<?, ?>) m;
                            Object url = mm.get("url");
                            Object type = mm.get("type");
                            return new ReviewDto.MediaDto(
                                    url == null ? null : String.valueOf(url),
                                    type == null ? "IMAGE" : String.valueOf(type));
                        }).toList();
            }
            return ResponseEntity.ok(ApiResponse.success(
                    "Cảm ơn bạn đã đánh giá sản phẩm!",
                    storefrontService.addReview(currentUser.getId(), id, rating, comment, media)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Tải ảnh/video đánh giá lên (JPG/PNG/GIF/WEBP ≤ 10MB; MP4/WEBM ≤ 50MB) */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadReviewMedia(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn tệp để tải lên.");
            }
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot).toLowerCase(Locale.ROOT);

            List<String> images = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
            List<String> videos = List.of(".mp4", ".webm", ".mov");
            String type;
            long max;
            if (images.contains(ext)) {
                type = "IMAGE";
                max = 10L * 1024 * 1024;
            } else if (videos.contains(ext)) {
                type = "VIDEO";
                max = 50L * 1024 * 1024;
            } else {
                throw new IllegalArgumentException("Chỉ chấp nhận ảnh (JPG, PNG, GIF, WEBP) hoặc video (MP4, WEBM, MOV).");
            }
            if (file.getSize() > max) {
                throw new IllegalArgumentException("Kích thước tệp tối đa là " + (max / 1024 / 1024) + "MB.");
            }
            Path dir = Paths.get(uploadDir, "reviews");
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ext;
            try (var in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
            return ResponseEntity.ok(ApiResponse.success("Tải tệp lên thành công!",
                    Map.of("url", "/uploads/reviews/" + filename, "type", type)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            log.error("Lỗi lưu tệp đánh giá: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Không thể lưu tệp, vui lòng thử lại."));
        }
    }

    // ==================== Giỏ hàng ====================

    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<List<com.storechain.group4.dto.CartItemDto>>> getCart(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy giỏ hàng thành công", storefrontService.getCart(currentUser.getId())));
    }

    @GetMapping("/cart/count")
    public ResponseEntity<ApiResponse<Long>> countCart(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Số sản phẩm trong giỏ", storefrontService.countCart(currentUser.getId())));
    }

    @PostMapping("/cart")
    public ResponseEntity<ApiResponse<com.storechain.group4.dto.CartItemDto>> addToCart(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            Long productId = body.get("productId") instanceof Number n ? n.longValue() : null;
            String size = body.get("size") == null ? "" : String.valueOf(body.get("size"));
            int quantity = body.get("quantity") instanceof Number n ? n.intValue() : 1;
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã thêm sản phẩm vào giỏ hàng!",
                    storefrontService.addToCart(currentUser.getId(), productId, size, quantity)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/cart/{itemId}")
    public ResponseEntity<ApiResponse<com.storechain.group4.dto.CartItemDto>> updateQuantity(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            int quantity = body.get("quantity") instanceof Number n ? n.intValue() : 1;
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật số lượng thành công!",
                    storefrontService.updateQuantity(currentUser.getId(), itemId, quantity)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<ApiResponse<String>> removeItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        storefrontService.removeItem(currentUser.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng!", null));
    }

    @DeleteMapping("/cart")
    public ResponseEntity<ApiResponse<String>> clearCart(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        storefrontService.clearCart(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng!", null));
    }

    // ==================== Đặt hàng ====================

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> checkout(
            @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            List<OrderDto> orders = storefrontService.checkout(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Đặt hàng thành công! Đơn hàng đang chờ chi nhánh xác nhận.",
                    orders));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách đơn hàng của bạn thành công",
                storefrontService.getMyOrders(currentUser.getId())));
    }
}
