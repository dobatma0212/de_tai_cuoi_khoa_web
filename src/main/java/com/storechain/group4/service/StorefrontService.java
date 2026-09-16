package com.storechain.group4.service;

import com.storechain.group4.dto.CartItemDto;
import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.OrderCreateRequest;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ReviewDto;

import java.util.List;

/**
 * Nghiệp vụ phía người mua: chi tiết sản phẩm, đánh giá, giỏ hàng, đặt hàng.
 */
public interface StorefrontService {

    ProductResponseDto getProductDetail(Long productId);

    List<ReviewDto> getProductReviews(Long productId);

    ReviewDto addReview(Long userId, Long productId, int rating, String comment,
                        List<ReviewDto.MediaDto> media);

    // ============ Giỏ hàng ============

    List<CartItemDto> getCart(Long userId);

    CartItemDto addToCart(Long userId, Long productId, String size, int quantity);

    CartItemDto updateQuantity(Long userId, Long itemId, int quantity);

    void removeItem(Long userId, Long itemId);

    void clearCart(Long userId);

    long countCart(Long userId);

    // ============ Đặt hàng từ giỏ ============

    /** Tách giỏ hàng thành các đơn theo từng chi nhánh (trạng thái NEW) */
    List<OrderDto> checkout(Long userId, OrderCreateRequest request);

    List<OrderDto> getMyOrders(Long userId);
}
