package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductHomeResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String image;
    private Integer soldQuantity;
    private String shopName;
}
