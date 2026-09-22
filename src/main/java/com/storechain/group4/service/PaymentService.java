package com.storechain.group4.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Tạo và xác minh giao dịch thanh toán qua cổng VNPay / MoMo.
 * Nếu chưa cấu hình key cổng thật, trả về trang mô phỏng nội bộ
 * để luồng demo vẫn chạy trọn vẹn.
 */
public interface PaymentService {

    /** Kết quả tạo URL thanh toán */
    record PaymentUrl(String url, boolean simulated) {}

    /**
     * Tạo URL thanh toán cho một mã giao dịch.
     *
     * @param paymentRef mã giao dịch (dùng chung cho các đơn của lần đặt)
     * @param amountVnd  tổng tiền VND
     * @param method     VNPAY hoặc MOMO
     * @param orderInfo  mô tả giao dịch hiển thị trên cổng
     * @param clientIp   IP khách hàng
     */
    PaymentUrl createPaymentUrl(String paymentRef, BigDecimal amountVnd, String method,
                                String orderInfo, String clientIp);

    /** Xác minh chữ ký secure hash của VNPay trả về (vnp_SecureHash) */
    boolean verifyVnpaySignature(Map<String, String> params);

    /** Xác minh chữ ký signature của MoMo trả về */
    boolean verifyMomoSignature(Map<String, String> params);

    /** Cổng đã cấu hình key thật chưa (false = dùng trang mô phỏng) */
    boolean isGatewayConfigured(String method);
}
