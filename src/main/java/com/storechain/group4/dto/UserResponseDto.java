package com.storechain.group4.dto;

import com.storechain.group4.model.RoleType;
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
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String status;
    private RoleType role;
    private String roleName;
    private String roleDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

