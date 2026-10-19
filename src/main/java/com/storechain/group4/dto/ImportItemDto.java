package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một dòng hàng trong phiếu nhập (kèm snapshot tên/SKU) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportItemDto {

    private Long id;
    private Long productId;
    private String productName;
    private String baseSku;
    private String size;
    private int quantity;
}
