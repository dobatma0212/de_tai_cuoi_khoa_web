package com.storechain.group4.service;

import com.storechain.group4.dto.ShopDetailDto;
import com.storechain.group4.dto.ShopRequestDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopStatsDto;
import com.storechain.group4.dto.UserResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ShopService {

    Page<ShopResponseDto> getShops(String keyword, String status, int page, int size, String sortBy, String sortDir);

    ShopStatsDto getStats();

    ShopResponseDto getShopById(Long id);

    ShopDetailDto getShopDetail(Long id);

    ShopResponseDto createShop(ShopRequestDto request);

    ShopResponseDto updateShop(Long id, ShopRequestDto request);

    ShopResponseDto updateStatus(Long id, String status);

    ShopResponseDto assignManager(Long shopId, Long managerId);

    void deleteShop(Long id);

    List<UserResponseDto> getAvailableManagers();
}
