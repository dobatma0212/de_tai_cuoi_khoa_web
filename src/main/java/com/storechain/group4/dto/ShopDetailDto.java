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
public class ShopDetailDto {

    private Long id;
    private String name;
    private String slug;
    private String address;
    private String phone;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Quản lý phụ trách (Manager)
    private Long managerId;
    private String managerUsername;
    private String managerFullName;
    private String managerEmail;
    private String managerPhone;

    // Chỉ số tóm tắt (Summary KPI)
    private BigDecimal currentMonthRevenue; // Doanh thu tháng hiện tại
    private Integer totalStock;             // Tổng số lượng hàng tồn
    private Long totalProducts;             // Số lượng mặt hàng
    private Long currentMonthOrders;        // Số đơn hoàn tất trong tháng
    private BigDecimal totalRevenue;        // Tổng doanh thu lũy kế
    private Long totalOrders;               // Tổng số đơn từ trước đến nay
    private Long totalEmployees;            // Tổng số nhân viên đang làm việc

    // Danh sách nhân viên làm việc trong chi nhánh
    @Builder.Default
    private List<BranchEmployeeItemDto> employees = new ArrayList<>();

    // Danh sách sản phẩm & số lượng tồn kho tại chi nhánh
    @Builder.Default
    private List<BranchProductStockItemDto> products = new ArrayList<>();

    // Danh sách đơn hàng gần đây
    @Builder.Default
    private List<BranchRecentOrderItemDto> recentOrders = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchProductStockItemDto {
        private Long productId;
        private String productName;
        private String baseSku;
        private String categoryName;
        private BigDecimal price;
        private String imageUrl;
        private int stock;
        private String sizeStocks;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchRecentOrderItemDto {
        private Long orderId;
        private String orderCode;
        private String customerName;
        private String customerPhone;
        private BigDecimal grandTotal;
        private String status;
        private String statusLabel;
        private String orderType;
        private String paymentMethod;
        private String paymentStatus;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchEmployeeItemDto {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private String status;
        private Boolean employed; // true: đang làm việc, false: đã nghỉ
        private String roleName;
        private String roleDescription;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
