package com.storechain.group4.service.impl;

import com.storechain.group4.model.Order;
import com.storechain.group4.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nhóm không làm role shipper nên giao hàng diễn ra tự động:
 * sau khi chi nhánh xác nhận, đơn tự chuyển Đang giao -> Đã giao
 * (khách vẫn có thể hủy giữa chừng; khách xác nhận đã nhận ở phía mình).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;

    /** Mỗi 5 giây đẩy đơn theo mốc tự động: CONFIRMED -> SHIPPING -> DELIVERED */
    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void autoAdvanceShipping() {
        List<Order> confirmed = orderRepository.findByStatus(Order.STATUS_CONFIRMED);
        for (Order o : confirmed) {
            o.setStatus(Order.STATUS_SHIPPING);
        }
        List<Order> shipping = orderRepository.findByStatus(Order.STATUS_SHIPPING);
        for (Order o : shipping) {
            o.setStatus(Order.STATUS_DELIVERED);
        }
        orderRepository.saveAll(confirmed);
        orderRepository.saveAll(shipping);
        if (!confirmed.isEmpty() || !shipping.isEmpty()) {
            log.info("Tự động cập nhật giao hàng: {} đơn -> Đang giao, {} đơn -> Đã giao",
                    confirmed.size(), shipping.size());
        }
    }
}
