package com.storechain.group4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống!")
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự!")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự!")
    private String receiverPhone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống!")
    @Size(max = 100, message = "Tỉnh/Thành phố tối đa 100 ký tự!")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống!")
    @Size(max = 100, message = "Quận/Huyện tối đa 100 ký tự!")
    private String district;

    @Size(max = 100, message = "Phường/Xã tối đa 100 ký tự!")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống!")
    @Size(max = 255, message = "Địa chỉ chi tiết tối đa 255 ký tự!")
    private String detailAddress;

    private Boolean isDefault;
}
