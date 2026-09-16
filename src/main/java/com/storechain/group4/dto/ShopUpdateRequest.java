package com.storechain.group4.dto;

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
public class ShopUpdateRequest {

    @Size(min = 1, max = 150, message = "Tên cửa hàng tối đa 150 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
    private String phone;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Size(max = 255, message = "Logo tối đa 255 ký tự")
    private String logoUrl;

    @Size(max = 255, message = "Banner tối đa 255 ký tự")
    private String bannerUrl;
}
