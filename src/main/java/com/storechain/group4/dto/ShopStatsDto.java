package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopStatsDto {

    private long totalShops;
    private long activeShops;
    private long inactiveShops;
    private long assignedShops;
}
