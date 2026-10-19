package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request tạo đơn hàng offline (bán trực tiếp tại quầy) do nhân viên nhập.
 * Giá và tên sản phẩm do hệ thống tự lấy theo sản phẩm của chi nhánh —
 * nhân viên chỉ được chọn sản phẩm, số lượng và size để tránh nhập sai.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineOrderRequest {

    private List<Item> items;

    /** Tên khách vãng lai (in lên hóa đơn) */
    private String customerName;

    /** Số điện thoại khách (không bắt buộc) */
    private String customerPhone;

    /** Tiền khách trả (để tính tiền thừa khi in hóa đơn, không bắt buộc) */
    private BigDecimal paidAmount;

    /** Ghi chú (không bắt buộc) */
    private String note;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long productId;
        private Integer quantity;
        /** Size chọn từ danh sách size của sản phẩm (nếu sản phẩm có size) */
        private String size;
    }
}
