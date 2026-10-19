package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantDto {

    /** Mã SKU con được tự động sinh (ví dụ: SM-TRANG-L) */
    private String sku;

    /** Màu sắc biến thể (ví dụ: Trắng) */
    private String color;

    /** Kích thước biến thể (ví dụ: L) */
    private String size;

    /** Ảnh đại diện riêng của màu sắc / biến thể */
    private String imageUrl;

    /** Mã vạch (Barcode) riêng nếu có */
    private String barcode;
}
