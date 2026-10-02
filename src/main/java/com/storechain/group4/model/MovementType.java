package com.storechain.group4.model;

/** Loại biến động kho: mỗi lần thay đổi tồn kho chi nhánh ghi một dòng */
public enum MovementType {
    /** Nhập hàng (phiếu nhập được xác nhận) */
    IMPORT,
    /** Hủy phiếu nhập đã xác nhận — trừ ngược tồn kho */
    IMPORT_CANCEL,
    /** Bán hàng (online hoặc tại quầy) */
    SALE,
    /** Hoàn tồn (hủy đơn, trả hàng) */
    RETURN
}
