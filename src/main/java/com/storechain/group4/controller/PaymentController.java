package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * API tạo link thanh toán + nhận callback từ VNPay / MoMo.
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Tạo URL thanh toán (gọi sau khi checkout tạo đơn thành công).
     * Body: { "method": "VNPAY"|"MOMO", "orderCode": "...", "amount": 150000, "orderInfo": "..." }
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String method = String.valueOf(body.getOrDefault("method", "COD"));
            String orderCode = String.valueOf(body.get("orderCode"));
            BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
            String orderInfo = body.get("orderInfo") != null ? String.valueOf(body.get("orderInfo")) : null;
            String returnUrl = body.get("returnUrl") != null ? String.valueOf(body.get("returnUrl")) : null;
            String ip = request.getRemoteAddr();

            Map<String, String> result = paymentService.createPayment(
                    method, orderCode, amount, orderInfo, returnUrl, ip);
            return ResponseEntity.ok(ApiResponse.success("Tạo link thanh toán thành công", result));
        } catch (Exception e) {
            log.error("createPayment error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** VNPay return URL (user browser redirect) */
    @GetMapping("/vnpay/return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        boolean ok = paymentService.handleCallback("VNPAY", params);
        String orderCode = params.getOrDefault("vnp_TxnRef", "");
        String target = ok
                ? "/profile.html?tab=orders&payment=success&order=" + orderCode
                : "/profile.html?tab=orders&payment=failed&order=" + orderCode;
        return new RedirectView(target);
    }

    /** MoMo return */
    @GetMapping("/momo/return")
    public RedirectView momoReturn(@RequestParam Map<String, String> params) {
        boolean ok = paymentService.handleCallback("MOMO", params);
        String orderCode = params.getOrDefault("orderId", "");
        String target = ok
                ? "/profile.html?tab=orders&payment=success&order=" + orderCode
                : "/profile.html?tab=orders&payment=failed&order=" + orderCode;
        return new RedirectView(target);
    }

    /** MoMo IPN (server-to-server) */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Map<String, Object>> momoIpn(@RequestBody Map<String, String> params) {
        boolean ok = paymentService.handleCallback("MOMO", params);
        Map<String, Object> resp = new HashMap<>();
        resp.put("resultCode", ok ? 0 : 1);
        resp.put("message", ok ? "Success" : "Failed");
        return ResponseEntity.ok(resp);
    }
}
