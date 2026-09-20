package com.storechain.group4.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class PromotionRequestDto {

    @NotBlank(message = "Loại khuyến mãi không được để trống")
    @Pattern(regexp = "^(FLASH_SALE|VOUCHER)$", message = "Loại khuyến mãi phải là FLASH_SALE hoặc VOUCHER")
    private String promotionType;

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 200, message = "Tên khuyến mãi không được vượt quá 200 ký tự")
    private String name;

    private String description;

    @Size(max = 50, message = "Mã khuyến mãi không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Loại giảm giá không được để trống")
    @Pattern(regexp = "^(PERCENTAGE|FIXED_AMOUNT)$", message = "Loại giảm giá phải là PERCENTAGE hoặc FIXED_AMOUNT")
    private String discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn hoặc bằng 0.01")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0", message = "Mức giảm tối đa không được âm")
    private BigDecimal maxDiscountAmount;

    @Min(value = 1, message = "Giới hạn sử dụng phải từ 1 trở lên")
    private Integer usageLimit;

    @Min(value = 1, message = "Giới hạn sử dụng mỗi người dùng phải từ 1 trở lên")
    private Integer usageLimitPerUser;

    @NotBlank(message = "Thời gian bắt đầu không được để trống")
    private String startDate;

    @NotBlank(message = "Thời gian kết thúc không được để trống")
    private String endDate;

    private String status;

    private List<Long> productIds;
}
