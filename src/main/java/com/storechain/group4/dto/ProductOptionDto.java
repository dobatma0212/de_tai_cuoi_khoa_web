package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOptionDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String categoryName;
    private Integer stock;
}
