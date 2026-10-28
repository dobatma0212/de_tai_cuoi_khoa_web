package com.storechain.group4.service.impl;

import com.storechain.group4.model.Order;
import com.storechain.group4.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nhóm không làm role shipper nên giao hàng diễn ra tự động.
 * Đơn phải được chi nhánh xác nhận trước, sau đó:
 *   Đã xác nhận qua {app.order.shipping-delay-minutes} phút -> Đang giao,
 *   Đang giao qua {app.order.deliver-delay-minutes} phút  -> Đã giao.
 * (khách vẫn có thể hủy giữa chừng; khách xác nhận đã nhận ở phía mình).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;

    @Value("${app.order.shipping-delay-minutes:2}")
    private long shippingDelayMinutes;

    @Value("${app.order.deliver-delay-minutes:5}")
    private long deliverDelayMinutes;

    /** Quét mỗi 15 giây (đủ nhanh cho mốc tính bằng phút, đỡ gọi DB từ xa liên tục) */
    @Scheduled(fixedDelay = 15_000)
    @Transactional
    public void autoAdvanceShipping() {
        LocalDateTime now = LocalDateTime.now();

        // Lấy cả hai danh sách TRƯỚC khi đổi trạng thái để một đơn không bị đẩy 2 mốc trong cùng một lượt
        List<Order> confirmed = orderRepository.findByStatus(Order.STATUS_CONFIRMED).stream()
                .filter(o -> o.getUpdatedAt() != null
                        && o.getUpdatedAt().plusMinutes(shippingDelayMinutes).isBefore(now))
                .toList();
        List<Order> shipping = orderRepository.findByStatus(Order.STATUS_SHIPPING).stream()
                .filter(o -> o.getUpdatedAt() != null
                        && o.getUpdatedAt().plusMinutes(deliverDelayMinutes).isBefore(now))
                .toList();

        confirmed.forEach(o -> o.setStatus(Order.STATUS_SHIPPING));
        shipping.forEach(o -> o.setStatus(Order.STATUS_DELIVERED));
        if (!confirmed.isEmpty()) orderRepository.saveAll(confirmed);
        if (!shipping.isEmpty()) orderRepository.saveAll(shipping);

        if (!confirmed.isEmpty() || !shipping.isEmpty()) {
            log.info("Tự động cập nhật giao hàng: {} đơn -> Đang giao, {} đơn -> Đã giao",
                    confirmed.size(), shipping.size());
        }
    }
}
