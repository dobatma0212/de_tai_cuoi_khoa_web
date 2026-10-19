package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Thông tin nhân viên chi nhánh (dành cho quản lý xem/quản lý) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {

    private Long id;

    private String username;

    private String fullName;

    private String email;

    private String phone;

    private String status;

    /** Còn đang là nhân viên chi nhánh (false = đã bị sa thải, chỉ còn là khách hàng) */
    private boolean employed;

    private LocalDateTime createdAt;
}
