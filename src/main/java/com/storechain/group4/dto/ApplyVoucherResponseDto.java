package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyVoucherResponseDto {

    private boolean valid;
    private Long promotionId;
    private String promotionName;
    private String code;
    /** PERCENT/SHIPPING — chỉ có với voucher của chi nhánh; null với voucher toàn server */
    private String promoType;
    /** Chi nhánh sở hữu voucher; null = voucher toàn server của admin */
    private String shopName;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
    private String message;
}
