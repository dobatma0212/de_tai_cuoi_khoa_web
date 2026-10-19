package com.storechain.group4.model;

/** Trạng thái phiếu nhập hàng */
public enum ReceiptStatus {
    /** Phiếu nháp — được sửa, có thể xác nhận hoặc hủy */
    DRAFT,
    /** Đã xác nhận — tồn kho chi nhánh đã được cộng */
    CONFIRMED,
    /** Đã hủy — không ảnh hưởng tồn kho */
    CANCELLED
}
