package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả đặt hàng từ giỏ:
 * - COD: chỉ cần danh sách đơn đã tạo.
 * - VNPAY/MOMO: kèm paymentUrl để chuyển hướng khách sang cổng thanh toán,
 *   paymentRef là mã giao dịch dùng chung cho các đơn tách từ lần đặt này.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResultDto {

    private List<OrderDto> orders = new ArrayList<>();
    private String paymentMethod;
    private String paymentRef;
    private BigDecimal totalGrandTotal;
    /** null nếu thanh toán COD */
    private String paymentUrl;
    /** true nếu URL là trang mô phỏng nội bộ (chưa cấu hình key cổng thật) */
    private boolean simulated;
}
