package com.storechain.group4.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu bổ nhiệm nhân viên của quản lý chi nhánh:
 * - Có userId: bổ nhiệm người dùng hiện có (đang là ROLE_USER) thành nhân viên của chi nhánh.
 * - Không có userId: tạo tài khoản nhân viên mới (cần username, email, password, fullName).
 */
@Getter
@Setter
public class HireEmployeeRequest {

    /** ID người dùng hiện có muốn bổ nhiệm (nếu có) */
    private Long userId;

    private String username;

    private String email;

    private String password;

    private String fullName;

    private String phone;
}
