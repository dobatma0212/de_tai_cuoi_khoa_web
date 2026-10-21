package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thanh toán online VNPAY / MoMo.
 * - /api/user/payments/init: khách lấy URL cổng thanh toán (cần đăng nhập).
 * - /api/public/payments/*: cổng gọi về (return qua trình duyệt, IPN từ máy chủ cổng), xác thực bằng chữ ký.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/user/payments/init")
    public ResponseEntity<ApiResponse<Map<String, Object>>> init(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {
        try {
            String ref = body.get("paymentRef") == null ? null : String.valueOf(body.get("paymentRef"));
            return ResponseEntity.ok(ApiResponse.success("Đã tạo giao dịch thanh toán",
                    paymentService.initPayment(currentUser.getId(), ref, clientIp(request))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== VNPAY ====================

    /** Trình duyệt được VNPAY chuyển về sau khi thanh toán */
    @GetMapping("/api/public/payments/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        String ref = params.get("vnp_TxnRef");
        if (!paymentService.verifyVnpay(params)) {
            return redirect("failed", ref, "invalid_signature");
        }
        boolean ok = paymentService.isVnpaySuccess(params);
        int result = paymentService.applyPaymentResult(ref, ok, paymentService.vnpayAmount(params));
        if (result == PaymentService.RESULT_BAD_AMOUNT || result == PaymentService.RESULT_NOT_FOUND) {
            return redirect("failed", ref, "order_mismatch");
        }
        return redirect(ok ? "success" : "failed", ref, null);
    }

    /** VNPAY gọi server-to-server; phải trả đúng định dạng RspCode/Message */
    @GetMapping("/api/public/payments/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        if (!paymentService.verifyVnpay(params)) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
        }
        int result = paymentService.applyPaymentResult(params.get("vnp_TxnRef"),
                paymentService.isVnpaySuccess(params), paymentService.vnpayAmount(params));
        return ResponseEntity.ok(switch (result) {
            case PaymentService.RESULT_OK -> Map.of("RspCode", "00", "Message", "Confirm Success");
            case PaymentService.RESULT_ALREADY_CONFIRMED -> Map.of("RspCode", "02", "Message", "Order already confirmed");
            case PaymentService.RESULT_BAD_AMOUNT -> Map.of("RspCode", "04", "Message", "Invalid amount");
            default -> Map.of("RspCode", "01", "Message", "Order not found");
        });
    }

    // ==================== MoMo ====================

    @GetMapping("/api/public/payments/momo-return")
    public ResponseEntity<Void> momoReturn(@RequestParam Map<String, String> params) {
        String ref = params.get("orderId");
        if (!paymentService.verifyMomo(params)) {
            return redirect("failed", ref, "invalid_signature");
        }
        boolean ok = paymentService.isMomoSuccess(params);
        int result = paymentService.applyPaymentResult(ref, ok, paymentService.momoAmount(params));
        if (result == PaymentService.RESULT_BAD_AMOUNT || result == PaymentService.RESULT_NOT_FOUND) {
            return redirect("failed", ref, "order_mismatch");
        }
        return redirect(ok ? "success" : "failed", ref, null);
    }

    /** MoMo gọi server-to-server (JSON); phản hồi HTTP 204 để MoMo ngừng gửi lại */
    @PostMapping("/api/public/payments/momo-ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> body) {
        Map<String, String> params = new LinkedHashMap<>();
        body.forEach((k, v) -> params.put(k, v == null ? "" : String.valueOf(v)));
        if (!paymentService.verifyMomo(params)) {
            log.warn("MoMo IPN sai chữ ký, orderId={}", params.get("orderId"));
            return ResponseEntity.badRequest().build();
        }
        paymentService.applyPaymentResult(params.get("orderId"),
                paymentService.isMomoSuccess(params), paymentService.momoAmount(params));
        return ResponseEntity.noContent().build();
    }

    // ==================== Tiện ích ====================

    private ResponseEntity<Void> redirect(String status, String ref, String reason) {
        StringBuilder url = new StringBuilder("/profile.html?tab=orders&payment=").append(status);
        if (ref != null) {
            url.append("&ref=").append(URLEncoder.encode(ref, StandardCharsets.UTF_8));
        }
        if (reason != null) {
            url.append("&reason=").append(reason);
        }
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url.toString()).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
