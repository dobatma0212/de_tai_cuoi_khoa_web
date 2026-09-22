package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Xem trước kết quả áp mã khuyến mãi cho giỏ hàng hiện tại (chưa đặt hàng).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoPreviewDto {

    private String promoCode;
    private String promoName;
    private boolean valid;

    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;

    /** Chi tiết theo từng chi nhánh sẽ tách đơn */
    @Builder.Default
    private List<ShopPreview> shops = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopPreview {
        private Long shopId;
        private String shopName;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal grandTotal;
        private String promoName;
        private String promoError;
    }
}
