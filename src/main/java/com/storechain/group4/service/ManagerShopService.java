package com.storechain.group4.service;

import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.PromotionDto;
import com.storechain.group4.dto.RevenueDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ManagerShopService {

    /** Lấy chi nhánh mà manager hiện đang được admin bổ nhiệm phụ trách (chỉ đọc, admin mới được sửa) */
    ShopResponseDto getMyShop(Long managerId);

    // ============ Danh mục sản phẩm của chi nhánh ============

    List<ShopCategoryDto> getMyCategories(Long managerId);

    ShopCategoryDto addCategory(Long managerId, String name);

    ShopCategoryDto renameCategory(Long managerId, Long categoryId, String name);

    void deleteCategory(Long managerId, Long categoryId);

    // ============ Sản phẩm chi nhánh ============

    List<ProductResponseDto> getMyProducts(Long managerId, Long categoryId);

    ProductResponseDto createProduct(Long managerId, ProductRequestDto request);

    ProductResponseDto updateProduct(Long managerId, Long productId, ProductRequestDto request);

    void deleteProduct(Long managerId, Long productId);

    /** Đặt/chỉ định sản phẩm làm đại diện cho danh mục của chi nhánh */
    ProductResponseDto setRepresentative(Long managerId, Long productId, boolean representative);

    /** Tải ảnh sản phẩm từ máy lên, trả về URL truy cập được */
    String uploadImage(MultipartFile file);

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
}
