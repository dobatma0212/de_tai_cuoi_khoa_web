package com.storechain.group4.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm tối đa 150 ký tự")
    private String name;

    /** Mã gốc sản phẩm (Base SKU / Barcode) */
    @Size(max = 100, message = "Mã Base SKU tối đa 100 ký tự")
    private String baseSku;

    @NotNull(message = "Vui lòng chọn danh mục cho sản phẩm")
    private Long categoryId;

    /** Thương hiệu sản phẩm */
    @Size(max = 150, message = "Thương hiệu tối đa 150 ký tự")
    private String brand;

    /** Tên bộ sưu tập */
    @Size(max = 150, message = "Bộ sưu tập tối đa 150 ký tự")
    private String collectionName;

    @NotNull(message = "Giá niêm yết không được để trống")
    @DecimalMin(value = "0", message = "Giá sản phẩm phải lớn hơn hoặc bằng 0 VND")
    private BigDecimal price;

    /** Ảnh đại diện chính */
    @Size(max = 500, message = "Đường dẫn hình ảnh tối đa 500 ký tự")
    private String imageUrl;

    /** Bộ sưu tập ảnh sản phẩm (Gallery) */
    private List<String> galleryImages;

    /** Bảng quy đổi kích cỡ (Size chart URL / text) */
    private String sizeChart;

    @Size(max = 10000, message = "Mô tả sản phẩm tối đa 10000 ký tự")
    private String description;

    /** Đường dẫn thân thiện SEO (Slug) */
    @Size(max = 250, message = "Slug tối đa 250 ký tự")
    private String slug;

    /** Thẻ tiêu đề SEO (Meta Title) */
    @Size(max = 250, message = "Meta Title tối đa 250 ký tự")
    private String metaTitle;

    /** Mô tả ngắn SEO (Meta Description) */
    @Size(max = 500, message = "Meta Description tối đa 500 ký tự")
    private String metaDescription;

    /** Các size, phân tách bằng dấu phẩy, VD: S,M,L,XL */
    @Size(max = 200, message = "Danh sách size tối đa 200 ký tự")
    private String sizes;

    /** Các màu, phân tách bằng dấu phẩy, VD: Đen, Trắng, Xanh */
    @Size(max = 250, message = "Danh sách màu tối đa 250 ký tự")
    private String color;

    /** Danh sách biến thể tự sinh với SKU, size, color, ảnh riêng */
    private List<ProductVariantDto> variants;

    /** Trạng thái kinh doanh: draft (Bản nháp), active (Đang mở bán), inactive (Ngừng kinh doanh) */
    private String status;

    /** Cờ Sản phẩm mới (New) */
    private Boolean isNew;

    /** Cờ Sản phẩm bán chạy (Best Seller) */
    private Boolean isBestSeller;

    /** Cờ Gợi ý phối đồ / Tiêu biểu (Featured) */
    private Boolean isFeatured;

    /** Sản phẩm đại diện */
    private Boolean representative;

    /** Tùy chọn (dùng cho API chi nhánh cũ nếu cần) */
    private Long shopId;
    private Integer stock;
}
