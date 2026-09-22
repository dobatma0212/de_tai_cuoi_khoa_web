package com.storechain.group4.service.impl;

import com.storechain.group4.model.Order;
import com.storechain.group4.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cài đặt VNPay (chuẩn query string + HMAC-SHA512) và MoMo (API captureMoMoWallet
 * + HMAC-SHA256). Khi chưa cấu hình key thật thì trả về trang mô phỏng nội bộ
 * /payment-simulate.html để chạy demo luồng thanh toán.
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${app.vnpay.tmn-code:}")
    private String vnpTmnCode;

    @Value("${app.vnpay.hash-secret:}")
    private String vnpHashSecret;

    @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${app.vnpay.return-url:http://localhost:8080/api/payments/vnpay/return}")
    private String vnpReturnUrl;

    @Value("${app.momo.partner-code:}")
    private String momoPartnerCode;

    @Value("${app.momo.access-key:}")
    private String momoAccessKey;

    @Value("${app.momo.secret-key:}")
    private String momoSecretKey;

    @Value("${app.momo.endpoint:https://test-payment.momo.vn/gw_payment/doipay}")
    private String momoEndpoint;

    @Value("${app.momo.return-url:http://localhost:8080/api/payments/momo/return}")
    private String momoReturnUrl;

    @Value("${app.momo.notify-url:http://localhost:8080/api/payments/momo/ipn}")
    private String momoNotifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isGatewayConfigured(String method) {
        if (Order.PAYMENT_VNPAY.equals(method)) {
            return !vnpTmnCode.isBlank() && !vnpHashSecret.isBlank();
        }
        if (Order.PAYMENT_MOMO.equals(method)) {
            return !momoPartnerCode.isBlank() && !momoAccessKey.isBlank() && !momoSecretKey.isBlank();
        }
        return false;
    }

    @Override
    public PaymentUrl createPaymentUrl(String paymentRef, BigDecimal amountVnd, String method,
                                       String orderInfo, String clientIp) {
        if (!isGatewayConfigured(method)) {
            // Chưa có key cổng thật: dùng trang mô phỏng nội bộ
            String url = "/payment-simulate.html?ref=" + urlEncode(paymentRef)
                    + "&method=" + urlEncode(method)
                    + "&amount=" + amountVnd.toPlainString();
            return new PaymentUrl(url, true);
        }
        if (Order.PAYMENT_VNPAY.equals(method)) {
            return new PaymentUrl(buildVnpayUrl(paymentRef, amountVnd, orderInfo, clientIp), false);
        }
        String url = buildMomoUrl(paymentRef, amountVnd, orderInfo);
        if (url == null) {
            // Gọi MoMo lỗi (mạng/sai key): fallback sang mô phỏng để khách không bị treo đơn
            log.warn("Gọi MoMo thất bại, chuyển sang trang mô phỏng cho giao dịch {}", paymentRef);
            return new PaymentUrl("/payment-simulate.html?ref=" + urlEncode(paymentRef)
                    + "&method=" + urlEncode(method)
                    + "&amount=" + amountVnd.toPlainString(), true);
        }
        return new PaymentUrl(url, false);
    }

    // ============ VNPay ============

    private String buildVnpayUrl(String paymentRef, BigDecimal amountVnd, String orderInfo, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount", String.valueOf(amountVnd.multiply(BigDecimal.valueOf(100)).longValueExact()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", paymentRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpReturnUrl);
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VNP_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNP_DATE));

        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
                hashData.append('&');
            }
            query.append(e.getKey()).append('=').append(urlEncode(e.getValue()));
            hashData.append(e.getKey()).append('=').append(urlEncode(e.getValue()));
        }
        String secureHash = hmacSha512(vnpHashSecret, hashData.toString());
        return vnpPayUrl + "?" + query + "&vnp_SecureHashType=SHA256&vnp_SecureHash=" + secureHash;
    }

    @Override
    public boolean verifyVnpaySignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank() || vnpHashSecret.isBlank()) {
            return false;
        }
        StringBuilder hashData = new StringBuilder();
        new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_"))
                .filter(e -> !e.getKey().equals("vnp_SecureHash")
                        && !e.getKey().equals("vnp_SecureHashType"))
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .forEach(e -> {
                    if (hashData.length() > 0) hashData.append('&');
                    hashData.append(e.getKey()).append('=').append(urlEncode(e.getValue()));
                });
        String computed = hmacSha512(vnpHashSecret, hashData.toString());
        return computed.equalsIgnoreCase(received);
    }

    // ============ MoMo ============

    private String buildMomoUrl(String paymentRef, BigDecimal amountVnd, String orderInfo) {
        try {
            String requestId = paymentRef;
            String rawSignature = "partnerCode=" + momoPartnerCode
                    + "&accessKey=" + momoAccessKey
                    + "&requestId=" + requestId
                    + "&amount=" + amountVnd.toPlainString()
                    + "&orderId=" + paymentRef
                    + "&returnUrl=" + momoReturnUrl
                    + "&notifyUrl=" + momoNotifyUrl
                    + "&extraData="
                    + "&requestType=captureMoMoWallet";
            String signature = hmacSha256(momoSecretKey, rawSignature);

            Map<String, String> body = new TreeMap<>();
            body.put("partnerCode", momoPartnerCode);
            body.put("accessKey", momoAccessKey);
            body.put("requestId", requestId);
            body.put("amount", amountVnd.toPlainString());
            body.put("orderId", paymentRef);
            body.put("orderInfo", orderInfo);
            body.put("returnUrl", momoReturnUrl);
            body.put("notifyUrl", momoNotifyUrl);
            body.put("extraData", "");
            body.put("requestType", "captureMoMoWallet");
            body.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(momoEndpoint,
                    new HttpEntity<>(body, headers), Map.class);
            if (resp != null && resp.containsKey("payUrl")) {
                return String.valueOf(resp.get("payUrl"));
            }
            log.warn("MoMo không trả payUrl: {}", resp);
            return null;
        } catch (Exception e) {
            log.error("Lỗi tạo URL MoMo: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean verifyMomoSignature(Map<String, String> params) {
        String received = params.get("signature");
        if (received == null || received.isBlank() || momoSecretKey.isBlank() || momoAccessKey.isBlank()) {
            return false;
        }
        String raw = "accessKey=" + momoAccessKey
                + "&amount=" + nv(params, "amount")
                + "&extraData=" + nv(params, "extraData")
                + "&message=" + nv(params, "message")
                + "&orderId=" + nv(params, "orderId")
                + "&orderInfo=" + nv(params, "orderInfo")
                + "&orderType=" + nv(params, "orderType")
                + "&partnerCode=" + nv(params, "partnerCode")
                + "&payType=" + nv(params, "payType")
                + "&requestId=" + nv(params, "requestId")
                + "&resultCode=" + nv(params, "resultCode")
                + "&transId=" + nv(params, "transId");
        return hmacSha256(momoSecretKey, raw).equalsIgnoreCase(received);
    }

    private static String nv(Map<String, String> params, String key) {
        String v = params.get(key);
        return v == null ? "" : v;
    }

    // ============ Common ============

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String hmacSha256(String key, String data) {
        return hmac("HmacSHA256", key, data);
    }

    private static String hmacSha512(String key, String data) {
        return hmac("HmacSHA512", key, data);
    }

    private static String hmac(String algorithm, String key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tính được chữ ký thanh toán: " + e.getMessage(), e);
        }
    }
}
