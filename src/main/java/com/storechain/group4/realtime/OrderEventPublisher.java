package com.storechain.group4.realtime;

import com.storechain.group4.model.Order;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * Đẩy sự kiện đơn hàng tới:
 *  - /topic/orders/user/{userId} : khách hàng sở hữu đơn
 *  - /topic/orders/shop/{shopId} : chi nhánh (manager / nhân viên / admin)
 * Chỉ gửi sau khi transaction commit để client không nhận dữ liệu chưa được lưu.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static OrderEventPublisher instance;

    private final SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    void register() {
        instance = this;
    }

    /** Dùng cho JPA entity listener (không được Spring inject). */
    public static void publish(String type, Order order) {
        if (instance == null || order == null) {
            return;
        }
        // Chỉ đọc id / trường cơ bản: không kích hoạt lazy-load trong lúc flush
        OrderEvent event = new OrderEvent(
                type,
                order.getId(),
                order.getOrderCode(),
                order.getShop() != null ? order.getShop().getId() : null,
                order.getUser() != null ? order.getUser().getId() : null,
                order.getStatus(),
                statusLabel(order.getStatus()),
                order.getPaymentStatus(),
                buildMessage(type, order),
                LocalDateTime.now());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    instance.send(event);
                }
            });
        } else {
            instance.send(event);
        }
    }

    private void send(OrderEvent event) {
        try {
            if (event.userId() != null) {
                messagingTemplate.convertAndSend("/topic/orders/user/" + event.userId(), event);
            }
            if (event.shopId() != null) {
                messagingTemplate.convertAndSend("/topic/orders/shop/" + event.shopId(), event);
            }
        } catch (Exception e) {
            log.warn("Không thể đẩy sự kiện đơn hàng {}: {}", event.orderCode(), e.getMessage());
        }
    }

    private static String buildMessage(String type, Order o) {
        return switch (type) {
            case "CREATED" -> "Đơn hàng mới " + o.getOrderCode();
            case "PAYMENT_CHANGED" -> "Đơn " + o.getOrderCode() + ": thanh toán " + o.getPaymentStatus();
            default -> "Đơn " + o.getOrderCode() + ": " + statusLabel(o.getStatus());
        };
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case Order.STATUS_NEW -> "Đơn hàng mới";
            case Order.STATUS_CONFIRMED -> "Đã xác nhận";
            case Order.STATUS_PICKED_UP -> "Đã lấy hàng";
            case Order.STATUS_SHIPPING -> "Đang giao";
            case Order.STATUS_DELIVERED -> "Đã giao";
            case Order.STATUS_RECEIVED -> "Khách đã nhận hàng";
            case Order.STATUS_CANCELLED -> "Đã hủy";
            case Order.STATUS_RETURN_REQUESTED -> "Yêu cầu trả hàng";
            case Order.STATUS_RETURNED -> "Đã trả hàng";
            default -> status;
        };
    }
}
