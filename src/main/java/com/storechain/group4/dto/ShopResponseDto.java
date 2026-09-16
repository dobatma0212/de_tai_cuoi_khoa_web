package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopResponseDto {

    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String phone;
    private String address;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;

    // Thông tin chủ shop (dùng cho admin)
    private Long ownerId;
    private String ownerUsername;
    private String ownerFullName;
    private String ownerEmail;
}
