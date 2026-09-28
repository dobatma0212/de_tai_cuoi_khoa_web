package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatsDto {

    private long totalProducts;
    private long activeProducts;
    private long draftProducts;
    private long inactiveProducts;
    private long outOfStockProducts;
    private long totalStock;
    private long totalShops;
    private long totalCategories;
}
