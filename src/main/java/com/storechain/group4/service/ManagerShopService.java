package com.storechain.group4.service;

import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.PromotionDto;
import com.storechain.group4.dto.RevenueDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;

import java.util.List;

public interface ManagerShopService {

    /** Lấy chi nhánh mà manager hiện đang được admin bổ nhiệm phụ trách (chỉ đọc, admin mới được sửa) */
    ShopResponseDto getMyShop(Long managerId);

    // ============ Danh mục sản phẩm của chi nhánh ============

    List<ShopCategoryDto> getMyCategories(Long managerId);

    // Manager chỉ được xem danh mục; thêm/sửa/xóa do admin và nhân viên thực hiện

    // ============ Sản phẩm chi nhánh (manager chỉ được xem) ============

    List<ProductResponseDto> getMyProducts(Long managerId, Long categoryId);

    // ============ Quản lý đơn hàng của chi nhánh ============

    List<OrderDto> getOrders(Long managerId, String status);

    /**
     * Manager xác nhận (CONFIRMED, kèm transporterId), hủy (CANCELLED, kèm lý do)
     * hoặc đánh dấu trả hàng (RETURNED) cho đơn của chi nhánh mình.
     */
    OrderDto updateOrderStatus(Long managerId, Long orderId, String status,
                               Long transporterId, String reason);

    // ============ Chương trình khuyến mãi ============

    List<PromotionDto> getPromotions(Long managerId);

    PromotionDto savePromotion(Long managerId, PromotionDto request);

    void deletePromotion(Long managerId, Long promoId);

    // ============ Doanh thu chi nhánh ============

    RevenueDto getRevenue(Long managerId);

    // ============ Quản lý nhân viên chi nhánh ============

    /** Danh sách nhân viên đang làm việc tại chi nhánh của quản lý */
    List<com.storechain.group4.dto.EmployeeDto> getMyEmployees(Long managerId);

    /**
     * Bổ nhiệm nhân viên: tạo tài khoản mới hoặc thăng ROLE_USER hiện có
     * thành ROLE_EMPLOYEE và gán vào chi nhánh của quản lý.
     */
    com.storechain.group4.dto.EmployeeDto hireEmployee(Long managerId,
            com.storechain.group4.dto.HireEmployeeRequest request);

    /**
     * Sa thải nhân viên: trả vai trò về ROLE_USER và gỡ khỏi chi nhánh
     * (không xóa tài khoản, giữ lịch sử đơn hàng).
     */
    void fireEmployee(Long managerId, Long employeeId);

    /**
     * Xem thống kê của một nhân viên (số công, đơn đã bán, doanh số, hoa hồng)
     * theo tháng — áp dụng cả nhân viên đang làm việc và đã bị sa thải,
     * để quản lý kiểm soát sau khi nhân viên nghỉ việc.
     */
    com.storechain.group4.dto.EmployeeStatsDto getEmployeeStats(Long managerId,
            Long employeeId, String month);
}
