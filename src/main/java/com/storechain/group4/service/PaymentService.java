package com.storechain.group4.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cổng thanh toán: COD (không cần gọi), VNPay, MoMo.
 * Skeleton – điền credentials thật trong application.properties để chạy production.
 */
public interface PaymentService {

    /**
     * Tạo URL thanh toán (redirect user sang cổng).
     * @return map chứa payUrl, orderCode, paymentRef ...
     */
    Map<String, String> createPayment(String method, String orderCode, BigDecimal amount,
                                      String orderInfo, String returnUrl, String ipAddr);

    /**
     * Xử lý callback / IPN từ cổng thanh toán.
     * @return true nếu thanh toán thành công
     */
    boolean handleCallback(String method, Map<String, String> params);

    /** Kiểm tra chữ ký / hash từ cổng */
    boolean verifySignature(String method, Map<String, String> params);
}
