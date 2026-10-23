package com.storechain.group4.dto;

import lombok.Data;

/**
 * Yêu cầu đặt hàng từ giỏ hàng của user:
 * toàn bộ sản phẩm trong giỏ được tách thành đơn theo từng chi nhánh.
 */
@Data
public class OrderCreateRequest {

    /** Mã khuyến mãi (tùy chọn, áp cho từng đơn tương ứng của chi nhánh đó) */
    private String promoCode;

    /** ID nhà vận chuyển */
    private Long transporterId;

    private String note;

    /** Phương thức thanh toán: COD (mặc định) / VNPAY / MOMO */
    private String paymentMethod;
}
