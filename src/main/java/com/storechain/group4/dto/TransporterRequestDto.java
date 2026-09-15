package com.storechain.group4.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransporterRequestDto {

    @NotBlank(message = "Tên đơn vị vận chuyển không được để trống")
    @Size(max = 100, message = "Tên đơn vị vận chuyển không được vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự")
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @DecimalMin(value = "0.0", inclusive = true, message = "Phí cơ bản không được âm")
    @Digits(integer = 8, fraction = 2, message = "Phí cơ bản không hợp lệ")
    private BigDecimal baseFee;

    @Pattern(regexp = "^(active|inactive)$", message = "Trạng thái chỉ có thể là 'active' hoặc 'inactive'")
    private String status;
}
