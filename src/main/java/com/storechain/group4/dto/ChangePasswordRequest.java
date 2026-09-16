package com.storechain.group4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại!")
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới!")
    @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 đến 100 ký tự!")
    private String newPassword;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới!")
    private String confirmNewPassword;
}
