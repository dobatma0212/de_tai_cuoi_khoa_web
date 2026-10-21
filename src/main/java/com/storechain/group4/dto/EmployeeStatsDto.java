package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Thống kê làm việc của nhân viên trong 1 tháng: số công, đơn đã bán, hoa hồng */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeStatsDto {

    /** Tháng thống kê, định dạng yyyy-MM */
    private String month;

    /** Số ngày đã chấm công trong tháng */
    private long workDays;

    /** Số đơn đã bán được (đã giao / khách đã nhận) trong tháng */
    private long ordersSold;

    /** Tổng giá trị các đơn đã bán được */
    private BigDecimal totalSales;

    /** Tỷ lệ hoa hồng (VD: 0.02 = 2%) */
    private BigDecimal commissionRate;

    /** Hoa hồng = totalSales * commissionRate */
    private BigDecimal commission;

    /** Chi tiết các đơn được tính hoa hồng */
    private List<OrderCommission> orders;

    /** Sao kê: TẤT CẢ đơn nhân viên phụ trách trong tháng (mọi trạng thái) */
    private List<OrderCommission> handledOrders;

    /** Các ngày đã chấm công trong tháng */
    private List<java.time.LocalDate> workDates;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderCommission {
        private Long orderId;
        private String orderCode;
        private String status;
        private BigDecimal grandTotal;
        private java.time.LocalDateTime createdAt;
    }
}
