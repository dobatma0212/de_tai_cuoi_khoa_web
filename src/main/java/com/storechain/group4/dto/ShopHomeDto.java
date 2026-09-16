package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dữ liệu trang chủ của một chi nhánh:
 * thông tin chi nhánh + các khối danh mục kèm sản phẩm đại diện.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopHomeDto {

    private ShopResponseDto shop;

    private List<CategoryBlock> categories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryBlock {
        /** ID danh mục của chi nhánh */
        private Long categoryId;
        /** Tên danh mục hiển thị */
        private String label;
        /** Sản phẩm đại diện của danh mục */
        private ProductResponseDto representativeProduct;
        private long totalProducts;
    }
}
