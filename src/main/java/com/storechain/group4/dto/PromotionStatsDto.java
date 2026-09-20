package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionStatsDto {

    private long totalPromotions;
    private long activePromotions;
    private long totalFlashSales;
    private long activeFlashSales;
    private long totalVouchers;
    private long activeVouchers;
    private long expiredPromotions;
}
