package com.storechain.group4.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyVoucherRequestDto {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotNull(message = "Tổng đơn hàng không hợp lệ")
    @DecimalMin(value = "0", message = "Tổng đơn hàng không hợp lệ")
    private BigDecimal orderTotal;
}
