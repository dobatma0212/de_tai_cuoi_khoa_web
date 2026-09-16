package com.storechain.group4.service;

import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopHomeDto;
import com.storechain.group4.dto.ShopResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ManagerShopService {

    /** Lấy chi nhánh mà manager hiện đang được admin bổ nhiệm phụ trách */
    ShopResponseDto getMyShop(Long managerId);

    /** Manager cập nhật thông tin chi nhánh của mình (không đổi trạng thái, không đổi manager) */
    ShopResponseDto updateMyShop(Long managerId, ShopResponseDto request);

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

    /** Dữ liệu trang chủ chi nhánh (dùng cho cả public) */
    ShopHomeDto getShopHome(Long shopId);

    /** Tìm kiếm sản phẩm theo từ khóa và/hoặc lọc theo danh mục */
    List<ProductResponseDto> searchShopProducts(Long shopId, String keyword, Long categoryId);

    List<ProductResponseDto> suggestShopProducts(Long shopId, String keyword);
}
