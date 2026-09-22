package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.model.Product;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.service.StorefrontCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Danh mục sản phẩm công khai cho người mua (không cần đăng nhập).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorefrontCatalogServiceImpl implements StorefrontCatalogService {

    private final ProductRepository productRepository;
    private final BranchStockRepository branchStockRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> listProducts(Long categoryId, String keyword) {
        List<Product> products = categoryId != null
                ? productRepository.findActiveProductsByCategory(categoryId)
                : productRepository.findActiveProducts();

        String kw = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        if (!kw.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getName() != null
                            && p.getName().toLowerCase(java.util.Locale.ROOT).contains(kw))
                    .collect(Collectors.toList());
        }
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> listRepresentativeProducts() {
        List<Product> products = productRepository.findActiveRepresentativeProducts();
        if (products.isEmpty()) {
            // Nếu chưa có sản phẩm đại diện nào thì trả về vài sản phẩm mới nhất
            products = productRepository.findActiveProducts().stream().limit(8).collect(Collectors.toList());
        }
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        return toDto(p);
    }

    private ProductResponseDto toDto(Product p) {
        Long shopId = null;
        String shopName = null;
        Long categoryId = null;
        String categoryName = null;
        try { if (p.getShop() != null) { shopId = p.getShop().getId(); shopName = p.getShop().getName(); } } catch (Exception ignored) { }
        try { if (p.getCategory() != null) { categoryId = p.getCategory().getId(); categoryName = p.getCategory().getName(); } } catch (Exception ignored) { }
        return ProductResponseDto.builder()
                .id(p.getId())
                .shopId(shopId)
                .shopName(shopName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .name(p.getName())
                .slug(p.getSlug())
                .price(p.getPrice())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .stock(branchStockRepository.sumStockByProductId(p.getId()))
                .sizes(p.getSizes())
                .sizesList(parseCsv(p.getSizes()))
                .color(p.getColor())
                .colorsList(parseCsv(p.getColor()))
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private Double round1(Double v) {
        if (v == null) return 0.0;
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
