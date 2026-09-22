package com.storechain.group4.service;

import com.storechain.group4.dto.ProductResponseDto;

import java.util.List;

/**
 * Danh mục sản phẩm hiển thị công khai (trang chủ, cửa hàng, chi tiết sản phẩm).
 */
public interface StorefrontCatalogService {

    /** Danh sách sản phẩm đang bán; lọc theo danh mục và/hoặc từ khóa nếu có */
    List<ProductResponseDto> listProducts(Long categoryId, String keyword);

    /** Sản phẩm đại diện hiển thị nổi bật ở trang chủ */
    List<ProductResponseDto> listRepresentativeProducts();

    /** Chi tiết một sản phẩm theo id */
    ProductResponseDto getProduct(Long productId);
}
