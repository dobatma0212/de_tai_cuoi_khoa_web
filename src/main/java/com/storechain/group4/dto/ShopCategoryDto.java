package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopCategoryDto {

    private Long id;
    private String name;
    private Integer sortOrder;
    private long totalProducts;

    /** Ảnh đại diện của danh mục (ảnh sản phẩm đại diện) */
    private String imageUrl;
}
