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
public class CartItemDto {

    private Long id;
    private Long productId;
    private String productName;
    private String imageUrl;
    private Long shopId;
    private String shopName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private boolean onFlashSale;
    private String flashSaleName;
    private Integer discountPercent;
    private String size;
    private String color;
    private int quantity;
    private int stock;
    private BigDecimal subtotal;
}
