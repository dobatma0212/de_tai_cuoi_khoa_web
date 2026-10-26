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
public class OrderDto {

    private Long id;
    private String orderCode;
    /** onl = đặt qua web, off = bán trực tiếp tại quầy */
    private String orderType;
    private Long shopId;
    private String shopName;
    private Long userId;
    private String customerName;
    private String status;
    private String statusLabel;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private String promoName;
    private String transporterName;
    private Long transporterId;
    private String cancelReason;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String note;
    /** COD / VNPAY / MOMO */
    private String paymentMethod;
    /** UNPAID / PENDING / PAID / FAILED */
    private String paymentStatus;
    /** Mã giao dịch dùng chung cho các đơn tách từ một lần thanh toán online */
    private String paymentRef;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Builder.Default
    private List<ItemDto> items = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDto {
        private Long productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
        private String size;
        private String color;
        private BigDecimal subtotal;
    }
}
