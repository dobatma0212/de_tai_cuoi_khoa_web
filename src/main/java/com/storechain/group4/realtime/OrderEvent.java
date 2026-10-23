package com.storechain.group4.realtime;

import java.time.LocalDateTime;

/**
 * Payload đẩy qua WebSocket khi đơn hàng được tạo hoặc thay đổi trạng thái.
 *
 * @param type          CREATED | STATUS_CHANGED | PAYMENT_CHANGED
 * @param message       nội dung thông báo hiển thị cho người dùng
 */
public record OrderEvent(
        String type,
        Long orderId,
        String orderCode,
        Long shopId,
        Long userId,
        String status,
        String statusLabel,
        String paymentStatus,
        String message,
        LocalDateTime timestamp
) {
}
