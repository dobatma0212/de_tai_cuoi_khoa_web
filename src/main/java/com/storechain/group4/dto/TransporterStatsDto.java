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
public class TransporterStatsDto {
    private long totalTransporters;
    private long activeTransporters;
    private long inactiveTransporters;
    private BigDecimal avgBaseFee;
}
