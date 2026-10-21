package com.storechain.group4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.Order;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Thanh toán online qua VNPAY / MoMo cho các đơn tách từ một lần đặt hàng
 * (cùng chia sẻ một paymentRef).
 *
 * Luồng: đặt hàng (paymentStatus = PENDING) -> initPayment() lấy URL cổng -> khách trả tiền
 * -> cổng gọi return/IPN -> applyPaymentResult() chuyển PAID hoặc FAILED.
 * Mỗi lần initPayment() sinh paymentRef mới vì cổng không nhận lại mã giao dịch đã dùng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    /** Kết quả xử lý callback từ cổng */
    public static final int RESULT_OK = 0;
    public static final int RESULT_NOT_FOUND = 1;
    public static final int RESULT_BAD_AMOUNT = 2;
    public static final int RESULT_ALREADY_CONFIRMED = 3;

    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Value("${payment.base-url:http://localhost:8080}")
    private String baseUrl;
    @Value("${payment.expire-minutes:30}")
    private long expireMinutes;

    @Value("${payment.vnpay.tmn-code:}")
    private String vnpTmnCode;
    @Value("${payment.vnpay.hash-secret:}")
    private String vnpHashSecret;
    @Value("${payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${payment.momo.partner-code:}")
    private String momoPartnerCode;
    @Value("${payment.momo.access-key:}")
    private String momoAccessKey;
    @Value("${payment.momo.secret-key:}")
    private String momoSecretKey;
    @Value("${payment.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;
    /** true: dùng cổng MoMo giả lập nội bộ (momo-mock.html), không gọi MoMo thật và không cần khóa */
    @Value("${payment.momo.mock:false}")
    private boolean momoMock;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchStockRepository branchStockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============ Cấu hình ============

    public boolean isOnline(String method) {
        return Order.PAYMENT_VNPAY.equals(method) || Order.PAYMENT_MOMO.equals(method);
    }

    /** Chặn sớm khi chọn cổng chưa cấu hình để không tạo đơn rồi mới báo lỗi */
    public void assertConfigured(String method) {
        if (Order.PAYMENT_VNPAY.equals(method) && (vnpTmnCode.isBlank() || vnpHashSecret.isBlank())) {
            throw new IllegalArgumentException(
                    "Cổng VNPAY chưa được cấu hình (payment.vnpay.tmn-code / payment.vnpay.hash-secret). Vui lòng chọn COD hoặc cổng khác.");
        }
        if (Order.PAYMENT_MOMO.equals(method) && !momoMock && (momoPartnerCode.isBlank()
                || momoAccessKey.isBlank() || momoSecretKey.isBlank())) {
            throw new IllegalArgumentException(
                    "Cổng MOMO chưa được cấu hình (payment.momo.*). Vui lòng chọn COD hoặc cổng khác.");
        }
    }

    public String generatePaymentRef() {
        return "P" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // ============ Khởi tạo thanh toán ============

    /**
     * Tạo URL thanh toán cho các đơn cùng paymentRef của user. Dùng được cho cả lần trả đầu tiên
     * lẫn "thanh toán lại" khi lần trước thất bại / khách đóng trang.
     */
    @Transactional
    public Map<String, Object> initPayment(Long userId, String paymentRef, String clientIp) {
        if (paymentRef == null || paymentRef.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã thanh toán.");
        }
        List<Order> orders = orderRepository.findByPaymentRef(paymentRef.trim());
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng cần thanh toán.");
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (Order o : orders) {
            if (!o.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Đây không phải đơn hàng của bạn.");
            }
            if (!isOnline(o.getPaymentMethod())) {
                throw new IllegalArgumentException("Đơn hàng này không thanh toán online.");
            }
            if (Order.PAY_STATUS_PAID.equals(o.getPaymentStatus())) {
                throw new IllegalArgumentException("Đơn hàng đã được thanh toán.");
            }
            if (Order.STATUS_CANCELLED.equals(o.getStatus())) {
                throw new IllegalArgumentException("Đơn hàng đã bị hủy, không thể thanh toán.");
            }
            amount = amount.add(o.getGrandTotal());
        }
        String method = orders.get(0).getPaymentMethod();
        assertConfigured(method);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
        }

        String newRef = generatePaymentRef();
        for (Order o : orders) {
            o.setPaymentRef(newRef);
            o.setPaymentStatus(Order.PAY_STATUS_PENDING);
        }
        orderRepository.saveAll(orders);

        String url = Order.PAYMENT_VNPAY.equals(method)
                ? buildVnpayUrl(newRef, amount, clientIp)
                : createMomoPayUrl(newRef, amount);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentUrl", url);
        data.put("paymentRef", newRef);
        data.put("method", method);
        return data;
    }

    // ============ Ghi nhận kết quả từ cổng ============

    /**
     * Cập nhật PAID/FAILED cho các đơn của paymentRef. paidAmount (VND) khác null thì phải khớp tổng đơn.
     * Không bao giờ hạ một đơn đã PAID xuống FAILED (callback có thể đến lặp hoặc lệch thứ tự).
     */
    @Transactional
    public int applyPaymentResult(String paymentRef, boolean success, BigDecimal paidAmount) {
        if (paymentRef == null || paymentRef.isBlank()) return RESULT_NOT_FOUND;
        List<Order> orders = orderRepository.findByPaymentRef(paymentRef);
        if (orders.isEmpty()) return RESULT_NOT_FOUND;

        BigDecimal expected = orders.stream().map(Order::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paidAmount != null && expected.compareTo(paidAmount) != 0) {
            log.warn("Sai số tiền thanh toán ref={} expected={} paid={}", paymentRef, expected, paidAmount);
            return RESULT_BAD_AMOUNT;
        }
        boolean allPaid = orders.stream().allMatch(o -> Order.PAY_STATUS_PAID.equals(o.getPaymentStatus()));
        if (allPaid) return RESULT_ALREADY_CONFIRMED;

        for (Order o : orders) {
            if (Order.PAY_STATUS_PAID.equals(o.getPaymentStatus())) continue;
            o.setPaymentStatus(success ? Order.PAY_STATUS_PAID : Order.PAY_STATUS_FAILED);
        }
        orderRepository.saveAll(orders);
        return RESULT_OK;
    }

    // ============ VNPAY ============

    private String buildVnpayUrl(String txnRef, BigDecimal amount, String clientIp) {
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", vnpTmnCode);
        p.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_OrderInfo", "Thanh toan don hang " + txnRef);
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", baseUrl + "/api/public/payments/vnpay-return");
        p.put("vnp_IpAddr", normalizeIp(clientIp));
        p.put("vnp_CreateDate", now.format(VNPAY_TIME));
        p.put("vnp_ExpireDate", now.plusMinutes(15).format(VNPAY_TIME));
        String query = vnpayQuery(p);
        return vnpPayUrl + "?" + query + "&vnp_SecureHash=" + hmac("HmacSHA512", vnpHashSecret, query);
    }

    /** Kiểm tra chữ ký callback VNPAY (return và IPN dùng chung) */
    public boolean verifyVnpay(Map<String, String> params) {
        if (vnpHashSecret.isBlank()) return false;
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;
        Map<String, String> signed = new TreeMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType")
                    && v != null && !v.isEmpty()) {
                signed.put(k, v);
            }
        });
        String expected = hmac("HmacSHA512", vnpHashSecret, vnpayQuery(signed));
        return expected.equalsIgnoreCase(received);
    }

    public boolean isVnpaySuccess(Map<String, String> params) {
        return "00".equals(params.get("vnp_ResponseCode"))
                && (params.get("vnp_TransactionStatus") == null || "00".equals(params.get("vnp_TransactionStatus")));
    }

    /** VNPAY gửi vnp_Amount = số tiền * 100 */
    public BigDecimal vnpayAmount(Map<String, String> params) {
        try {
            return new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));
        } catch (Exception e) {
            return null;
        }
    }

    private String vnpayQuery(Map<String, String> sortedParams) {
        List<String> parts = new ArrayList<>();
        sortedParams.forEach((k, v) -> parts.add(
                URLEncoder.encode(k, StandardCharsets.US_ASCII) + "="
                        + URLEncoder.encode(v, StandardCharsets.US_ASCII)));
        return String.join("&", parts);
    }

    // ============ MoMo ============

    private String createMomoPayUrl(String ref, BigDecimal amount) {
        if (momoMock) {
            // Địa chỉ tương đối: chạy đúng cả trên localhost lẫn ngrok
            return "/momo-mock.html?ref=" + URLEncoder.encode(ref, StandardCharsets.UTF_8);
        }
        String amountStr = amount.toBigInteger().toString();
        String orderInfo = "Thanh toan don hang " + ref;
        String redirectUrl = baseUrl + "/api/public/payments/momo-return";
        String ipnUrl = baseUrl + "/api/public/payments/momo-ipn";
        String extraData = "";
        String requestType = "captureWallet";

        String raw = "accessKey=" + momoAccessKey + "&amount=" + amountStr + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl + "&orderId=" + ref + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoPartnerCode + "&redirectUrl=" + redirectUrl
                + "&requestId=" + ref + "&requestType=" + requestType;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", momoPartnerCode);
        body.put("partnerName", "StoreChain");
        body.put("storeId", "StoreChain");
        body.put("requestId", ref);
        body.put("amount", Long.parseLong(amountStr));
        body.put("orderId", ref);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("lang", "vi");
        body.put("requestType", requestType);
        body.put("autoCapture", true);
        body.put("extraData", extraData);
        body.put("signature", hmac("HmacSHA256", momoSecretKey, raw));

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(momoEndpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (json.path("resultCode").asInt(-1) != 0 || json.path("payUrl").asText("").isBlank()) {
                log.warn("MoMo từ chối tạo giao dịch: {}", response.body());
                throw new IllegalArgumentException("MoMo không tạo được giao dịch: "
                        + json.path("message").asText("lỗi không xác định"));
            }
            return json.path("payUrl").asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Kết nối MoMo bị gián đoạn, vui lòng thử lại.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi gọi MoMo: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Không kết nối được MoMo, vui lòng thử lại sau.");
        }
    }

    /** Kiểm tra chữ ký callback MoMo (redirect và IPN dùng chung) */
    public boolean verifyMomo(Map<String, String> p) {
        if (effMomoSecretKey().isBlank()) return false;
        String received = p.get("signature");
        if (received == null || received.isBlank()) return false;
        return hmac("HmacSHA256", effMomoSecretKey(), momoSignatureRaw(p)).equalsIgnoreCase(received);
    }

    /** Chuỗi gốc để ký/kiểm chữ ký kết quả thanh toán MoMo (các trường theo thứ tự chữ cái) */
    private String momoSignatureRaw(Map<String, String> p) {
        return "accessKey=" + effMomoAccessKey()
                + "&amount=" + nz(p.get("amount"))
                + "&extraData=" + nz(p.get("extraData"))
                + "&message=" + nz(p.get("message"))
                + "&orderId=" + nz(p.get("orderId"))
                + "&orderInfo=" + nz(p.get("orderInfo"))
                + "&orderType=" + nz(p.get("orderType"))
                + "&partnerCode=" + nz(p.get("partnerCode"))
                + "&payType=" + nz(p.get("payType"))
                + "&requestId=" + nz(p.get("requestId"))
                + "&responseTime=" + nz(p.get("responseTime"))
                + "&resultCode=" + nz(p.get("resultCode"))
                + "&transId=" + nz(p.get("transId"));
    }

    // Ở chế độ mock mà chưa điền khóa thật thì dùng khóa nội bộ để vẫn ký và kiểm chữ ký được
    private String effMomoAccessKey() {
        return momoMock && momoAccessKey.isBlank() ? "MOCK_ACCESS_KEY" : momoAccessKey;
    }

    private String effMomoSecretKey() {
        return momoMock && momoSecretKey.isBlank() ? "MOCK_SECRET_KEY" : momoSecretKey;
    }

    private String effMomoPartnerCode() {
        return momoMock && momoPartnerCode.isBlank() ? "MOCK_MOMO" : momoPartnerCode;
    }

    public boolean isMomoSuccess(Map<String, String> p) {
        return "0".equals(p.get("resultCode"));
    }

    public BigDecimal momoAmount(Map<String, String> p) {
        try {
            return new BigDecimal(p.get("amount"));
        } catch (Exception e) {
            return null;
        }
    }

    // ============ MoMo giả lập ============

    public boolean isMomoMock() {
        return momoMock;
    }

    /** Thông tin hiển thị trên trang MoMo giả: số tiền và các đơn của lần thanh toán */
    @Transactional(readOnly = true)
    public Map<String, Object> mockMomoInfo(Long userId, String paymentRef) {
        List<Order> orders = loadMockOrders(userId, paymentRef);
        BigDecimal amount = orders.stream().map(Order::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentRef", paymentRef);
        data.put("amount", amount);
        data.put("orderCodes", orders.stream().map(Order::getOrderCode).toList());
        data.put("paid", orders.stream().allMatch(o -> Order.PAY_STATUS_PAID.equals(o.getPaymentStatus())));
        return data;
    }

    /**
     * Trang MoMo giả báo kết quả (resultCode theo quy ước MoMo: 0 = thành công; 1001 không đủ tiền;
     * 1003 giao dịch bị hủy; 1005 hết hạn; 1006 người dùng từ chối). Server dựng bộ tham số có chữ ký
     * giống MoMo thật: vừa áp kết quả ngay (vai trò IPN), vừa trả URL return để trình duyệt đi qua
     * đúng đường kiểm chữ ký của /momo-return.
     */
    @Transactional
    public Map<String, Object> confirmMockMomo(Long userId, String paymentRef, int resultCode, String payType) {
        List<Order> orders = loadMockOrders(userId, paymentRef);
        BigDecimal amount = orders.stream().map(Order::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean success = resultCode == 0;
        String message = switch (resultCode) {
            case 0 -> "Giao dịch thành công.";
            case 1001 -> "Giao dịch thất bại do tài khoản không đủ tiền.";
            case 1003 -> "Giao dịch đã bị hủy.";
            case 1005 -> "Giao dịch đã hết hạn.";
            case 1006 -> "Giao dịch bị từ chối bởi người dùng.";
            default -> "Giao dịch thất bại.";
        };
        long now = System.currentTimeMillis();
        String transId = success ? String.valueOf(now) : "0";

        Map<String, String> p = new LinkedHashMap<>();
        p.put("partnerCode", effMomoPartnerCode());
        p.put("orderId", paymentRef);
        p.put("requestId", paymentRef);
        p.put("amount", amount.toBigInteger().toString());
        p.put("orderInfo", "Thanh toan don hang " + paymentRef);
        p.put("orderType", "momo_wallet");
        p.put("transId", transId);
        p.put("resultCode", String.valueOf(resultCode));
        p.put("message", message);
        p.put("payType", payType == null || payType.isBlank() ? "webApp" : payType);
        p.put("responseTime", String.valueOf(now));
        p.put("extraData", "");
        p.put("signature", hmac("HmacSHA256", effMomoSecretKey(), momoSignatureRaw(p)));

        applyPaymentResult(paymentRef, success, amount);

        List<String> parts = new ArrayList<>();
        p.forEach((k, v) -> parts.add(k + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", success);
        data.put("resultCode", resultCode);
        data.put("message", message);
        data.put("transId", transId);
        data.put("amount", amount);
        data.put("orderCodes", orders.stream().map(Order::getOrderCode).toList());
        data.put("responseTime", now);
        data.put("redirectUrl", "/api/public/payments/momo-return?" + String.join("&", parts));
        return data;
    }

    /** Chỉ cho thao tác khi bật mock, đơn thuộc về chính người dùng và là đơn MoMo */
    private List<Order> loadMockOrders(Long userId, String paymentRef) {
        if (!momoMock) {
            throw new IllegalArgumentException("Cổng MoMo giả lập đang tắt.");
        }
        List<Order> orders = paymentRef == null ? List.of() : orderRepository.findByPaymentRef(paymentRef.trim());
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch.");
        }
        for (Order o : orders) {
            if (!o.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Đây không phải giao dịch của bạn.");
            }
            if (!Order.PAYMENT_MOMO.equals(o.getPaymentMethod())) {
                throw new IllegalArgumentException("Giao dịch này không phải MoMo.");
            }
        }
        return orders;
    }

    // ============ Hết hạn thanh toán ============

    /**
     * Đơn online chưa được thanh toán sau expireMinutes phút thì tự hủy và hoàn tồn kho,
     * tránh giữ hàng mãi khi khách bỏ dở trang thanh toán.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpiredUnpaidOrders() {
        LocalDateTime limit = LocalDateTime.now().minusMinutes(expireMinutes);
        List<Order> expired = new ArrayList<>();
        expired.addAll(orderRepository.findByPaymentStatusAndStatusAndCreatedAtBefore(
                Order.PAY_STATUS_PENDING, Order.STATUS_NEW, limit));
        expired.addAll(orderRepository.findByPaymentStatusAndStatusAndCreatedAtBefore(
                Order.PAY_STATUS_FAILED, Order.STATUS_NEW, limit));
        for (Order o : expired) {
            if (!isOnline(o.getPaymentMethod())) continue;
            o.setStatus(Order.STATUS_CANCELLED);
            o.setCancelReason("Quá hạn thanh toán online (hệ thống tự hủy)");
            for (var oi : orderItemRepository.findByOrderId(o.getId())) {
                if (oi.getProduct() == null) continue;
                BranchStock bs = branchStockRepository
                        .findByProductIdAndShopId(oi.getProduct().getId(), o.getShop().getId())
                        .orElse(BranchStock.builder().product(oi.getProduct())
                                .shop(o.getShop()).stock(0).build());
                bs.setStock(bs.getStock() + oi.getQuantity());
                branchStockRepository.save(bs);
            }
            orderRepository.save(o);
            log.info("Tự hủy đơn {} do quá hạn thanh toán online", o.getOrderCode());
        }
    }

    // ============ Tiện ích ============

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank() || ip.contains(":")) return "127.0.0.1";
        return ip.trim();
    }

    private String hmac(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký thanh toán", e);
        }
    }
}
