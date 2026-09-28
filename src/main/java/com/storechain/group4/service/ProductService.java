package com.storechain.group4.service;

import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ProductStatsDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    Page<ProductResponseDto> getProducts(
            String keyword,
            Long shopId,
            Long categoryId,
            String status,
            Boolean inStock,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ProductStatsDto getStats();

    ProductResponseDto getProductById(Long id);

    ProductResponseDto createProduct(ProductRequestDto request);

    ProductResponseDto updateProduct(Long id, ProductRequestDto request);

    ProductResponseDto updateStatus(Long id, String status);

    ProductResponseDto updateStock(Long id, int stock);

    ProductResponseDto setRepresentative(Long id, boolean representative);

    ProductResponseDto toggleBadge(Long id, String badge, boolean value);

    void deleteProduct(Long id);

    String uploadImage(MultipartFile file);

    List<ShopResponseDto> getAvailableShops();

    List<ShopCategoryDto> getCategoriesByShop(Long shopId);
}
