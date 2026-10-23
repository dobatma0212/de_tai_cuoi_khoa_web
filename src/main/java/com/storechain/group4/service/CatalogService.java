package com.storechain.group4.service;

import com.storechain.group4.dto.ProductPageDto;
import com.storechain.group4.dto.ProductResponseDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Danh mục sản phẩm toàn hãng (trang chủ hãng):
 * người dùng mua sắm trên một danh mục chung, đơn hàng sau đó được
 * hệ thống phân phối về chi nhánh gần địa chỉ nhận hàng nhất.
 */
public interface CatalogService {

    /**
     * Danh sách sản phẩm toàn hãng có lọc + phân trang:
     * từ khóa, danh mục (ShopCategory), khoảng giá, size, sắp xếp.
     */
    ProductPageDto searchProducts(String keyword, Long categoryId,
                                  BigDecimal minPrice, BigDecimal maxPrice,
                                  String size, String color, String sort, int page, int pageSize);

    /** Gợi ý sản phẩm khi gõ từ khóa (tối đa 8 kết quả) */
    List<ProductResponseDto> suggestProducts(String keyword);

    /** Danh mục sản phẩm toàn hãng (gộp theo tên, dùng cho bộ lọc danh mục) */
    List<com.storechain.group4.dto.ShopCategoryDto> getCategories();

    /** Chi tiết một sản phẩm của catalog (chuyển tiếp sang chi tiết sản phẩm) */
    ProductResponseDto getProduct(Long productId);

    /**
     * Top 20 sản phẩm theo tiêu chí, có phân trang trong phạm vi 20 sản phẩm đó.
     * type: new (mới nhất), bestseller (bán chạy), toprated (đánh giá cao), favorite (yêu thích nhiều).
     */
    ProductPageDto topProducts(String type, int page, int pageSize);

    /** Chuyển danh sách sản phẩm thành thẻ sản phẩm (kèm tồn kho, đánh giá, lượt bán, lượt thích) */
    List<ProductResponseDto> toCards(List<com.storechain.group4.model.Product> products);
}
