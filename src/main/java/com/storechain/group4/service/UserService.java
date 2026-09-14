package com.storechain.group4.service;

import com.storechain.group4.dto.CreateUserRequest;
import com.storechain.group4.dto.UpdateUserRequest;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.dto.UserStatsDto;
import com.storechain.group4.security.services.CustomUserDetails;
import org.springframework.data.domain.Page;

public interface UserService {

    Page<UserResponseDto> getUsers(String keyword, String role, String status, int page, int size, String sortBy, String sortDir);

    UserResponseDto getUserById(Long id);

    UserResponseDto createUser(CreateUserRequest request, CustomUserDetails currentUser);

    UserResponseDto updateUser(Long id, UpdateUserRequest request, CustomUserDetails currentUser);

    UserResponseDto updateStatus(Long id, String status, CustomUserDetails currentUser);

    void deleteUser(Long id, CustomUserDetails currentUser);

    UserStatsDto getUserStats();
}

