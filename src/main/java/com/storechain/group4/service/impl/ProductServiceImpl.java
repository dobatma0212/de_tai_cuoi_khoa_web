package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ProductHomeResponse;
import com.storechain.group4.model.Product;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    // Ngưỡng "bán từ 10 sản phẩm trở lên" theo yêu cầu trang chủ
    private static final int MIN_SOLD_QUANTITY = 10;

    private final ProductRepository productRepository;

    @Override
    public List<ProductHomeResponse> getBestSellingProducts() {
        return productRepository
                .findBySoldQuantityGreaterThanEqualOrderBySoldQuantityDesc(MIN_SOLD_QUANTITY)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProductHomeResponse toResponse(Product product) {
        return new ProductHomeResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImage(),
                product.getSoldQuantity(),
                product.getShop() != null ? product.getShop().getName() : null
        );
    }
}
