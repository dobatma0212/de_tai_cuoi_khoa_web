package com.storechain.group4.service.impl;

import com.storechain.group4.model.Order;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Optional;

/**
 * Skeleton tích hợp VNPay & MoMo.
 * - VNPay: sandbox https://sandbox.vnpayment.vn
 * - MoMo:  sandbox https://test-payment.momo.vn
 *
 * Điền các key thật vào application.properties (hoặc biến môi trường) trước khi lên production.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;

    // ===== VNPay config =====
    @Value("${app.payment.vnpay.tmn-code:}")
    private String vnpTmnCode;
    @Value("${app.payment.vnpay.hash-secret:}")
    private String vnpHashSecret;
    @Value("${app.payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;
    @Value("${app.payment.vnpay.return-url:http://localhost:8080/api/payment/vnpay/return}")
    private String vnpReturnUrl;

    // ===== MoMo config =====
    @Value("${app.payment.momo.partner-code:}")
    private String momoPartnerCode;
    @Value("${app.payment.momo.access-key:}")
    private String momoAccessKey;
    @Value("${app.payment.momo.secret-key:}")
    private String momoSecretKey;
    @Value("${app.payment.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;
    @Value("${app.payment.momo.return-url:http://localhost:8080/api/payment/momo/return}")
    private String momoReturnUrl;
    @Value("${app.payment.momo.notify-url:http://localhost:8080/api/payment/momo/ipn}")
    private String momoNotifyUrl;

    @Override
    public Map<String, String> createPayment(String method, String orderCode, BigDecimal amount,
                                             String orderInfo, String returnUrl, String ipAddr) {
        String m = method == null ? "COD" : method.trim().toUpperCase();
        return switch (m) {
            case "VNPAY" -> createVnPayUrl(orderCode, amount, orderInfo, returnUrl, ipAddr);
            case "MOMO"  -> createMomoUrl(orderCode, amount, orderInfo, returnUrl);
            default      -> Map.of(
                    "method", "COD",
                    "message", "Thanh toán khi nhận hàng – không cần redirect",
                    "orderCode", orderCode
            );
        };
    }

    // ==================== VNPay ====================

    private Map<String, String> createVnPayUrl(String orderCode, BigDecimal amount,
                                               String orderInfo, String returnUrl, String ipAddr) {
        if (vnpTmnCode == null || vnpTmnCode.isBlank() || vnpHashSecret == null || vnpHashSecret.isBlank()) {
            log.warn("VNPay chưa cấu hình tmn-code / hash-secret – trả về URL giả lập");
            return Map.of(
                    "method", "VNPAY",
                    "payUrl", "#vnpay-not-configured",
                    "orderCode", orderCode,
                    "message", "VNPay chưa cấu hình. Điền app.payment.vnpay.* trong application.properties"
            );
        }

        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", vnpTmnCode);
            // VNPay yêu cầu amount * 100 (đơn vị xu)
            params.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", orderCode);
            params.put("vnp_OrderInfo", orderInfo != null ? orderInfo : ("Thanh toan don " + orderCode));
            params.put("vnp_OrderType", "other");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl != null && !returnUrl.isBlank() ? returnUrl : vnpReturnUrl);
            params.put("vnp_IpAddr", ipAddr != null ? ipAddr : "127.0.0.1");
            params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (!hashData.isEmpty()) {
                    hashData.append('&');
                    query.append('&');
                }
                String encKey = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
                String encVal = URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8);
                hashData.append(encKey).append('=').append(encVal);
                query.append(encKey).append('=').append(encVal);
            }
            String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(secureHash);

            String payUrl = vnpPayUrl + "?" + query;
            return Map.of(
                    "method", "VNPAY",
                    "payUrl", payUrl,
                    "orderCode", orderCode,
                    "paymentRef", orderCode
            );
        } catch (Exception e) {
            log.error("Lỗi tạo URL VNPay: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo link thanh toán VNPay");
        }
    }

    // ==================== MoMo ====================

    private Map<String, String> createMomoUrl(String orderCode, BigDecimal amount,
                                              String orderInfo, String returnUrl) {
        if (momoPartnerCode == null || momoPartnerCode.isBlank()) {
            log.warn("MoMo chưa cấu hình – trả về URL giả lập");
            return Map.of(
                    "method", "MOMO",
                    "payUrl", "#momo-not-configured",
                    "orderCode", orderCode,
                    "message", "MoMo chưa cấu hình. Điền app.payment.momo.* trong application.properties"
            );
        }
        // Skeleton: thực tế cần gọi HTTP POST tới momoEndpoint với chữ ký HMAC
        // Ở đây trả về cấu trúc chuẩn để frontend/redirect dùng.
        String requestId = orderCode + "-" + System.currentTimeMillis();
        return Map.of(
                "method", "MOMO",
                "payUrl", momoEndpoint + "?orderId=" + orderCode + "&requestId=" + requestId,
                "orderCode", orderCode,
                "paymentRef", requestId,
                "message", "MoMo skeleton – cần hoàn thiện gọi API create với chữ ký khi có credentials"
        );
    }

    // ==================== Callback ====================

    @Override
    @Transactional
    public boolean handleCallback(String method, Map<String, String> params) {
        if (!verifySignature(method, params)) {
            log.warn("Chữ ký callback {} không hợp lệ: {}", method, params);
            return false;
        }
        String m = method == null ? "" : method.trim().toUpperCase();
        String orderCode;
        boolean success;
        if ("VNPAY".equals(m)) {
            orderCode = params.get("vnp_TxnRef");
            String resp = params.get("vnp_ResponseCode");
            success = "00".equals(resp);
        } else if ("MOMO".equals(m)) {
            orderCode = params.get("orderId");
            String resultCode = params.get("resultCode");
            success = "0".equals(resultCode);
        } else {
            return false;
        }
        if (orderCode == null || orderCode.isBlank()) return false;

        OptionalOrder(orderCode).ifPresent(order -> {
            if (success) {
                order.setPaymentStatus("PAID");
                order.setPaymentRef(params.getOrDefault("vnp_TransactionNo",
                        params.getOrDefault("transId", orderCode)));
            } else {
                order.setPaymentStatus("FAILED");
            }
            orderRepository.save(order);
            log.info("Cập nhật thanh toán đơn {} -> {}", orderCode, order.getPaymentStatus());
        });
        return success;
    }

    private Optional<Order> OptionalOrder(String orderCode) {
        return orderRepository.findAll().stream()
                .filter(o -> orderCode.equals(o.getOrderCode()))
                .findFirst();
    }

    @Override
    public boolean verifySignature(String method, Map<String, String> params) {
        String m = method == null ? "" : method.trim().toUpperCase();
        try {
            if ("VNPAY".equals(m)) {
                String received = params.get("vnp_SecureHash");
                if (received == null || vnpHashSecret == null || vnpHashSecret.isBlank()) return false;
                Map<String, String> sorted = new TreeMap<>(params);
                sorted.remove("vnp_SecureHash");
                sorted.remove("vnp_SecureHashType");
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> e : sorted.entrySet()) {
                    if (e.getValue() != null && !e.getValue().isBlank()) {
                        if (!sb.isEmpty()) sb.append('&');
                        sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                          .append('=')
                          .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
                    }
                }
                String calc = hmacSHA512(vnpHashSecret, sb.toString());
                return calc.equalsIgnoreCase(received);
            }
            if ("MOMO".equals(m)) {
                // Skeleton: production cần verify HMAC với secretKey
                return true; // tạm chấp nhận khi chưa cấu hình
            }
        } catch (Exception e) {
            log.error("Lỗi verify chữ ký: {}", e.getMessage());
        }
        return false;
    }

    private static String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
