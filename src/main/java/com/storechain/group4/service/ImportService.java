package com.storechain.group4.service;

import com.storechain.group4.dto.ImportReceiptDto;
import com.storechain.group4.dto.ImportReceiptRequestDto;

import java.util.List;

/**
 * Nhập hàng (stock receipts) cho manager chi nhánh:
 * lập phiếu nháp -> xác nhận (cộng tồn kho chi nhánh + ghi biến động) hoặc hủy.
 * Admin chỉ xem được qua dữ liệu, không có endpoint ghi ở đây.
 */
public interface ImportService {

    /** Danh sách phiếu nhập của chi nhánh, lọc theo trạng thái / khoảng ngày tạo */
    List<ImportReceiptDto> getMyReceipts(Long managerId, String status, String fromDate, String toDate);

    ImportReceiptDto getReceipt(Long managerId, Long receiptId);

    /** Lập phiếu nháp mới (DRAFT) cho chi nhánh của manager */
    ImportReceiptDto createReceipt(Long managerId, ImportReceiptRequestDto request);

    /** Sửa phiếu khi còn DRAFT */
    ImportReceiptDto updateReceipt(Long managerId, Long receiptId, ImportReceiptRequestDto request);

    /** Xác nhận phiếu DRAFT: cộng tồn kho chi nhánh + ghi movement IMPORT */
    ImportReceiptDto confirmReceipt(Long managerId, Long receiptId);

    /**
     * Hủy phiếu: DRAFT -> CANCELLED trực tiếp;
     * CONFIRMED chỉ hủy được nếu tồn hiện tại đủ để trừ ngược, ngược lại báo lỗi.
     */
    ImportReceiptDto cancelReceipt(Long managerId, Long receiptId);
}
