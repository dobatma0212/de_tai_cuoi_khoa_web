package com.storechain.group4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thông tin sản phẩm trả về cho phía người mua (trang chủ, cửa hàng, chi tiết).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {

    private Long id;
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String slug;
    private BigDecimal price;
    private String description;
    private String imageUrl;
    private Integer stock;
    /** Chuỗi size gốc, phân tách bởi dấu phẩy */
    private String sizes;

    @Builder.Default
    private List<String> sizesList = new ArrayList<>();

    /** Chuỗi màu gốc, phân tách bởi dấu phẩy */
    private String color;

    @Builder.Default
    private List<String> colorsList = new ArrayList<>();

    private boolean representative;
    private String status;
    private Double avgRating;
    private Long reviewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
