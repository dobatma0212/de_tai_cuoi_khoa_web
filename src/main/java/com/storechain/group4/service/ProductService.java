package com.storechain.group4.service;

import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import org.springframework.data.domain.Page;

public interface ProductService {

    Page<ProductResponseDto> getMyProducts(Long userId, String keyword, String status, Long categoryId,
                                           int page, int size, String sortBy, String sortDir);

    ProductResponseDto getMyProductById(Long userId, Long productId);

    ProductResponseDto createMyProduct(Long userId, ProductRequestDto request);

    ProductResponseDto updateMyProduct(Long userId, Long productId, ProductRequestDto request);

    ProductResponseDto updateMyProductStatus(Long userId, Long productId, String status);

    void deleteMyProduct(Long userId, Long productId);

    // === Public ===
    Page<ProductResponseDto> getPublicProducts(Long shopId, String keyword, int page, int size);
}
