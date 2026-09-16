package com.storechain.group4.service;

import com.storechain.group4.dto.ShopRegistrationRequest;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopUpdateRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ShopService {

    // === Người dùng (profile) ===
    List<ShopResponseDto> getMyShops(Long userId);

    ShopResponseDto registerShop(ShopRegistrationRequest request, Long userId);

    // === Vendor ===
    List<ShopResponseDto> getMyActiveShops(Long userId);

    ShopResponseDto getMyShop(Long userId, Long shopId);

    ShopResponseDto updateMyShop(ShopUpdateRequest request, Long userId, Long shopId);

    // === Admin ===
    Page<ShopResponseDto> getShops(String keyword, String status, int page, int size, String sortBy, String sortDir);

    ShopResponseDto getShopById(Long id);

    ShopResponseDto approveShop(Long id);

    ShopResponseDto rejectShop(Long id, String reason);

    ShopResponseDto updateShopStatus(Long id, String status);

    // === Public ===
    Page<ShopResponseDto> getActiveShops(String keyword, int page, int size);

    ShopResponseDto getPublicShop(Long id);
}
