package com.storechain.group4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponseDto {

    private Long id;
    private String promotionType;
    /** PERCENT/SHIPPING — chỉ có với voucher của chi nhánh; null với voucher toàn server */
    private String promoType;
    private String name;
    private String description;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usageLimitPerUser;
    private int usedCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    private String status;
    /** Chi nhánh sở hữu voucher; null = voucher toàn server của admin */
    private Long shopId;
    private String shopName;
    private boolean expired;
    private boolean currentlyActive;
    private Integer remainingUsage;

    @Builder.Default
    private List<Long> productIds = new ArrayList<>();

    @Builder.Default
    private List<ProductOptionDto> products = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
