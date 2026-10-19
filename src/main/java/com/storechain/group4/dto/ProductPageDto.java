package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kết quả danh sách sản phẩm có phân trang + bộ lọc (dùng cho trang chủ chi nhánh).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPageDto {

    private List<ProductResponseDto> content;
    private int page;
    private int pageSize;
    private long totalElements;
    private int totalPages;
}
