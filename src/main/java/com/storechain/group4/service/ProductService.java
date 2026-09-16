package com.storechain.group4.service;

import com.storechain.group4.dto.ProductResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponseDto.PageDto getProducts(String keyword, String categorySlug, String sort, Pageable pageable);

    ProductResponseDto getProductBySlug(String slug);

    List<ProductResponseDto> getRelatedProducts(String slug, int limit);
}
