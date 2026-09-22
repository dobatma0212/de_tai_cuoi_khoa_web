package com.storechain.group4.controller;

import com.storechain.group4.model.Order;
import com.storechain.group4.service.PaymentService;
import com.storechain.group4.service.StorefrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

/**
 * Nhận kết quả trả về từ cổng thanh toán VNPay / MoMo (return URL + IPN)
 * và trang mô phỏng nội bộ khi chưa cấu hình key cổng thật.
 * Các endpoint phải public vì cổng thanh toán gọi trực tiếp (không kèm JWT).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final StorefrontService storefrontService;

    private static RedirectView result(String pay, String ref) {
        return new RedirectView("/checkout.html?pay=" + pay + "&ref=" + ref, true);
    }

    // ==================== VNPay ====================

    /** VNPay redirect khách về sau khi thanh toán (kiểm secure hash rồi cập nhật đơn) */
    @GetMapping("/vnpay/return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        String ref = params.getOrDefault("vnp_TxnRef", "");
        try {
            boolean ok = paymentService.verifyVnpaySignature(params)
                    && "00".equals(params.get("vnp_ResponseCode"));
            storefrontService.markPaymentResult(ref, ok);
            return result(ok ? "success" : "failed", ref);
        } catch (Exception e) {
            log.error("Lỗi xử lý VNPay return cho {}: {}", ref, e.getMessage(), e);
            return result("failed", ref);
        }
    }

    /** VNPay IPN (server-to-server): trả JSON chuẩn VNPay */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> rsp = new HashMap<>();
        String ref = params.getOrDefault("vnp_TxnRef", "");
        try {
            if (!paymentService.verifyVnpaySignature(params)) {
                rsp.put("RspCode", "97");
                rsp.put("Message", "Invalid Signature");
            } else {
                storefrontService.markPaymentResult(ref, "00".equals(params.get("vnp_ResponseCode")));
                rsp.put("RspCode", "00");
                rsp.put("Message", "Confirm Success");
            }
        } catch (Exception e) {
            log.error("Lỗi VNPay IPN cho {}: {}", ref, e.getMessage(), e);
            rsp.put("RspCode", "99");
            rsp.put("Message", "Unknown error");
        }
        return ResponseEntity.ok(rsp);
    }

    // ==================== MoMo ====================

    /** MoMo redirect khách về sau khi thanh toán (kiểm signature rồi cập nhật đơn) */
    @GetMapping("/momo/return")
    public RedirectView momoReturn(@RequestParam Map<String, String> params) {
        String ref = params.getOrDefault("orderId", "");
        try {
            boolean ok = params.containsKey("signature")
                    ? paymentService.verifyMomoSignature(params)
                      && "0".equals(params.get("resultCode"))
                    : "0".equals(params.get("resultCode"));
            storefrontService.markPaymentResult(ref, ok);
            return result(ok ? "success" : "failed", ref);
        } catch (Exception e) {
            log.error("Lỗi xử lý MoMo return cho {}: {}", ref, e.getMessage(), e);
            return result("failed", ref);
        }
    }

    /** MoMo IPN (server-to-server) */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Map<String, Object>> momoIpn(@RequestBody Map<String, String> params) {
        String ref = params.getOrDefault("orderId", "");
        try {
            storefrontService.markPaymentResult(ref, "0".equals(params.get("resultCode")));
            return ResponseEntity.ok(Map.of(
                    "partnerCode", params.getOrDefault("partnerCode", ""),
                    "orderId", ref,
                    "requestId", params.getOrDefault("requestId", ""),
                    "resultCode", 0,
                    "message", "Success",
                    "responseTime", System.currentTimeMillis(),
                    "extraData", ""));
        } catch (Exception e) {
            log.error("Lỗi MoMo IPN cho {}: {}", ref, e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "orderId", ref,
                    "resultCode", 99,
                    "message", "Error",
                    "responseTime", System.currentTimeMillis()));
        }
    }

    // ==================== Trang mô phỏng (demo khi chưa có key cổng thật) ====================

    /**
     * Xác nhận/thất bại giao dịch từ trang mô phỏng nội bộ.
     * Chỉ hoạt động khi cổng chưa cấu hình key thật.
     */
    @GetMapping("/simulate/{ref}")
    public RedirectView simulate(@PathVariable String ref, @RequestParam(defaultValue = "true") boolean success,
                                 @RequestParam String method) {
        if (paymentService.isGatewayConfigured(method)) {
            // Đã cấu hình cổng thật thì không cho mô phỏng
            return result("failed", ref);
        }
        storefrontService.markPaymentResult(ref, success);
        return result(success ? "success" : "failed", ref);
    }
}
