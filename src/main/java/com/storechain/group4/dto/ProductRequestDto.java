package com.storechain.group4.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn danh mục cho sản phẩm")
    private Long categoryId;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0", message = "Giá sản phẩm phải lớn hơn hoặc bằng 0 VND")
    private BigDecimal price;

    @Size(max = 5000, message = "Mô tả sản phẩm tối đa 5000 ký tự")
    private String description;

    @Size(max = 500, message = "Đường dẫn hình ảnh tối đa 500 ký tự")
    private String imageUrl;

    @Min(value = 0, message = "Tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stock;

    /** Các size cách nhau bởi dấu phẩy, VD: S,M,L,XL (tùy chọn) */
    @Size(max = 200, message = "Danh sách size tối đa 200 ký tự")
    private String sizes;
}
