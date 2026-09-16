package com.storechain.group4.dto;

import jakarta.validation.constraints.Email;
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
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống!")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự!")
    private String fullName;

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không đúng định dạng!")
    @Size(max = 100, message = "Email tối đa 100 ký tự!")
    private String email;

    @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự!")
    private String phone;

    @Size(max = 255, message = "Ảnh đại diện (URL) tối đa 255 ký tự!")
    private String avatarUrl;
}
