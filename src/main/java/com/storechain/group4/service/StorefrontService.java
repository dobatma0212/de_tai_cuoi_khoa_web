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

    CartItemDto addToCart(Long userId, Long productId, String size, String color, int quantity);

    CartItemDto updateQuantity(Long userId, Long itemId, int quantity);

    void removeItem(Long userId, Long itemId);

    void clearCart(Long userId);

    long countCart(Long userId);

    // ============ Đặt hàng từ giỏ ============

    /**
     * Tách giỏ hàng thành các đơn theo từng chi nhánh (trạng thái NEW).
     * Nếu thanh toán VNPAY/MOMO: đơn ở trạng thái thanh toán PENDING và kết quả
     * kèm paymentUrl để chuyển hướng khách sang cổng.
     */
    com.storechain.group4.dto.CheckoutResultDto checkout(Long userId, OrderCreateRequest request,
                                                         String clientIp);

    /** Xem trước giảm giá khi áp mã khuyến mãi cho giỏ hàng hiện tại (chưa đặt hàng) */
    com.storechain.group4.dto.PromoPreviewDto previewPromo(Long userId, String promoCode,
                                                           Long transporterId);

    /**
     * Cổng thanh toán trả về kết quả cho mã giao dịch:
     * thành công -> PENDING thành PAID; thất bại -> hủy các đơn PENDING và hoàn tồn kho.
     *
     * @return số đơn đã cập nhật
     */
    int markPaymentResult(String paymentRef, boolean success);

    List<OrderDto> getMyOrders(Long userId);

    /** Khách hủy đơn (kèm lý do) khi đơn chưa được giao tới */
    OrderDto cancelMyOrder(Long userId, Long orderId, String reason);

    /** Khách xác nhận đã nhận hàng (DELIVERED -> RECEIVED) */
    OrderDto receiveMyOrder(Long userId, Long orderId);

    /** Khách đòi trả hàng - hoàn tiền (DELIVERED/RECEIVED -> RETURN_REQUESTED, chờ chi nhánh duyệt) */
    OrderDto requestReturn(Long userId, Long orderId, String reason);
}
