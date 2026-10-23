package com.storechain.group4.realtime;

import com.storechain.group4.model.Order;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

import java.util.Objects;

/**
 * Bắt mọi thay đổi của Order tại một chỗ (khách / chi nhánh / scheduler / thanh toán)
 * nên không cần sửa từng service.
 */
public class OrderEntityListener {

    @PostLoad
    public void onLoad(Order o) {
        o.setSnapshotStatus(o.getStatus());
        o.setSnapshotPaymentStatus(o.getPaymentStatus());
    }

    @PostPersist
    public void onCreate(Order o) {
        o.setSnapshotStatus(o.getStatus());
        o.setSnapshotPaymentStatus(o.getPaymentStatus());
        OrderEventPublisher.publish("CREATED", o);
    }

    @PostUpdate
    public void onUpdate(Order o) {
        boolean statusChanged = !Objects.equals(o.getSnapshotStatus(), o.getStatus());
        boolean paymentChanged = !Objects.equals(o.getSnapshotPaymentStatus(), o.getPaymentStatus());
        o.setSnapshotStatus(o.getStatus());
        o.setSnapshotPaymentStatus(o.getPaymentStatus());
        if (statusChanged) {
            OrderEventPublisher.publish("STATUS_CHANGED", o);
        } else if (paymentChanged) {
            OrderEventPublisher.publish("PAYMENT_CHANGED", o);
        }
    }
}
