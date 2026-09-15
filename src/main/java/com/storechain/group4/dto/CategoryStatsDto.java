package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryStatsDto {
    private long totalCategories;
    private long activeCategories;
    private long inactiveCategories;
    private long rootCategories;
}
