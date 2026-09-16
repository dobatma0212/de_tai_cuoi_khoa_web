package com.storechain.group4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    private Integer stock;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    @Size(max = 255, message = "Ảnh tối đa 255 ký tự")
    private String imageUrl;

    private String status;
}
