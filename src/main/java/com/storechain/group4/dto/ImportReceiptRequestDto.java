package com.storechain.group4.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Yêu cầu tạo / sửa phiếu nhập hàng (manager). Chi nhánh lấy từ tài khoản, không nhận từ request. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReceiptRequestDto {

    @Size(max = 255, message = "Nguồn nhập tối đa 255 ký tự.")
    private String sourceNote;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.")
    private String note;

    @NotEmpty(message = "Phiếu nhập phải có ít nhất một dòng hàng.")
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {

        @NotNull(message = "Vui lòng chọn sản phẩm cho từng dòng nhập.")
        private Long productId;

        /** Size nhập (bắt buộc nếu sản phẩm có phân size) */
        @Size(max = 20, message = "Size tối đa 20 ký tự.")
        private String size;

        @Min(value = 1, message = "Số lượng nhập phải ít nhất là 1.")
        @NotNull(message = "Vui lòng nhập số lượng.")
        private Integer quantity;
    }
}
