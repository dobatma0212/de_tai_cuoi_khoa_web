package com.storechain.group4.service;

import com.storechain.group4.dto.EmployeeStatsDto;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * API dành cho nhân viên chi nhánh (ROLE_EMPLOYEE):
 * - Thêm/sửa/xóa sản phẩm của chi nhánh mình được phân công
 * - Xem hoa hồng theo đơn hàng đã bán được
 * - Chấm công / tính công theo tháng
 */
public interface EmployeeService {

    // ============ Chi nhánh của nhân viên ============

    ShopResponseDto getMyShop(Long employeeId);

    List<ShopCategoryDto> getMyCategories(Long employeeId);

    // ============ Sản phẩm của chi nhánh ============

    List<ProductResponseDto> getMyProducts(Long employeeId, Long categoryId);

    ProductResponseDto createProduct(Long employeeId, ProductRequestDto request);

    ProductResponseDto updateProduct(Long employeeId, Long productId, ProductRequestDto request);

    void deleteProduct(Long employeeId, Long productId);

    ProductResponseDto setRepresentative(Long employeeId, Long productId, boolean representative);

    String uploadImage(MultipartFile file);

    // ============ Đơn hàng của chi nhánh (như manager) ============

    List<com.storechain.group4.dto.OrderDto> getMyOrders(Long employeeId, String status);

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
