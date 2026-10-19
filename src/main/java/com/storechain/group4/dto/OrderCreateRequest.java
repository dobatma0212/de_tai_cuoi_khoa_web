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

    /** ID địa chỉ nhận hàng đã lưu (ưu tiên dùng nếu có) */
    private Long shippingAddressId;

    /**
     * Phương thức thanh toán: COD | VNPAY | MOMO
     * Mặc định COD nếu không gửi.
     */
    private String paymentMethod;

    private String note;

    /** Thông tin địa chỉ nhận hàng (dùng khi không chọn shippingAddressId) */
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String district;
    private String ward;
    private String detailAddress;
}
