package com.storechain.group4.service;

import com.storechain.group4.dto.EmployeeStatsDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopResponseDto;

import java.util.List;

/**
 * API dành cho nhân viên chi nhánh (ROLE_EMPLOYEE):
 * - Xem chi nhánh, sản phẩm, đơn hàng của chi nhánh mình
 * - Bán tại quầy (đơn offline), chấm công / tính hoa hồng
 */
public interface EmployeeService {

    // ============ Chi nhánh của nhân viên ============

    ShopResponseDto getMyShop(Long employeeId);

    // ============ Sản phẩm của chi nhánh ============

    List<ProductResponseDto> getMyProducts(Long employeeId, Long categoryId);

    // ============ Đơn hàng của chi nhánh (như manager) ============

    List<com.storechain.group4.dto.OrderDto> getMyOrders(Long employeeId, String status);

    /**
     * Tạo đơn hàng offline (bán trực tiếp tại quầy): nhân viên chỉ chọn sản phẩm
     * của chi nhánh mình + số lượng + size; giá/tên sản phẩm do hệ thống tự lấy,
     * tồn kho chi nhánh bị trừ ngay, đơn hoàn tất (DELIVERED) và gắn handledBy
     * là nhân viên nhập nên được tính hoa hồng như đơn online đã giao.
     */
    com.storechain.group4.dto.OrderDto createOfflineOrder(Long employeeId,
                                                          com.storechain.group4.dto.OfflineOrderRequest request);

    /**
     * Nhân viên cập nhật trạng thái đơn (giới hạn như manager):
     * xác nhận (CONFIRMED, kèm transporterId — đơn được gắn handledBy là nhân viên
     * để tính hoa hồng khi giao thành công), hủy (CANCELLED, kèm lý do),
     * trả hàng (RETURNED). Lý do hủy/trả bắt buộc khi cần.
     */
    com.storechain.group4.dto.OrderDto updateOrderStatus(Long employeeId, Long orderId, String status,
                                                         Long transporterId, String reason);

    // ============ Chấm công & hoa hồng ============

    EmployeeStatsDto checkIn(Long employeeId);

    EmployeeStatsDto getMyStats(Long employeeId, String month);
}
