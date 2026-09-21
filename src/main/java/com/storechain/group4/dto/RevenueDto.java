package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueDto {

    /** Doanh thu = tổng tiền các đơn đã giao (DELIVERED) */
    private BigDecimal totalRevenue;

    /** Tiền hàng đã giao (chưa tính phí VC/khuyến mãi) */
    private BigDecimal deliveredProductAmount;

    /** Tổng phí vận chuyển các đơn đã giao */
    private BigDecimal deliveredShippingFee;

    /** Tổng tiền khuyến mãi giảm cho các đơn đã giao */
    private BigDecimal deliveredDiscount;

    /** Phí vận chuyển phải trả cho đơn vị vận chuyển của các đơn bị hủy/trả (làm giảm doanh thu) */
    private BigDecimal shippingLoss;

    private long totalOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long returnedOrders;

    /** Số đơn theo từng trạng thái */
    private Map<String, Long> statusCounts;
}
