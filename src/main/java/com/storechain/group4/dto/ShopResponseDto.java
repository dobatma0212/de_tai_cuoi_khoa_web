package com.storechain.group4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopResponseDto {

    private Long id;
    private String name;
    private String slug;
    private String address;
    private String phone;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Thông tin Quản lý phụ trách (Manager)
    private Long managerId;
    private String managerUsername;
    private String managerFullName;
    private String managerEmail;
    private String managerPhone;
}
