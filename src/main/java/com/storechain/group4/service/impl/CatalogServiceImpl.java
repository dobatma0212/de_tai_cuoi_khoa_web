package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ProductPageDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.model.Product;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.ShopCategoryRepository;
import com.storechain.group4.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ProductRepository productRepository;
    private final BranchStockRepository branchStockRepository;
    private final ProductReviewRepository reviewRepository;
    private final ShopCategoryRepository shopCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductPageDto searchProducts(String keyword, Long categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String size, String color, String sort, int page, int pageSize) {
        String kw = keyword == null ? "" : keyword.trim();
        List<Product> products = productRepository.searchAllProducts(kw);

        // Lọc trong bộ nhớ (khối lượng dữ liệu demo nhỏ nên chấp nhận được)
        List<Product> filtered = products.stream()
                .filter(p -> categoryId == null || categoryId <= 0
                        || (p.getCategory() != null && p.getCategory().getId().equals(categoryId)))
                .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                .filter(p -> size == null || size.isBlank()
                        || sizeMatches(p.getSizes(), size.trim()))
                .filter(p -> color == null || color.isBlank()
                        || (p.getColor() != null && p.getColor().toLowerCase(java.util.Locale.ROOT)
                                .contains(color.trim().toLowerCase(java.util.Locale.ROOT))))
                .collect(Collectors.toList());

        if ("priceAsc".equals(sort)) {
            filtered.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
        } else if ("priceDesc".equals(sort)) {
            filtered.sort((a, b) -> b.getPrice().compareTo(a.getPrice()));
        } else if ("name".equals(sort)) {
            filtered.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        } // mặc định: mới nhất

        int safePageSize = pageSize <= 0 ? 12 : pageSize;
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) filtered.size() / safePageSize);
        int from = Math.min(safePage * safePageSize, filtered.size());
        int to = Math.min(from + safePageSize, filtered.size());

        List<ProductResponseDto> content = filtered.subList(from, to)
                .stream().map(this::toCatalogDto).collect(Collectors.toList());

        return ProductPageDto.builder()
                .content(content)
                .page(safePage)
                .pageSize(safePageSize)
                .totalElements(filtered.size())
                .totalPages(totalPages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> suggestProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return productRepository.suggestAllByName(keyword.trim())
                .stream().limit(8).map(this::toCatalogDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.storechain.group4.dto.ShopCategoryDto> getCategories() {
        // Gộp các danh mục trùng tên của các chi nhánh thành danh mục toàn hãng
        java.util.Map<String, com.storechain.group4.dto.ShopCategoryDto> byName = new java.util.LinkedHashMap<>();
        shopCategoryRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(c -> c.getShop().getId()))
                .forEach(c -> {
                    String key = c.getName().trim().toLowerCase(java.util.Locale.ROOT);
                    long count = productRepository.countByCategoryId(c.getId());
                    byName.compute(key, (k, existing) -> {
                        if (existing == null) {
                            return com.storechain.group4.dto.ShopCategoryDto.builder()
                                    .id(c.getId())
                                    .name(c.getName())
                                    .sortOrder(c.getSortOrder())
                                    .totalProducts(count)
                                    .imageUrl(representativeImageUrl(c.getId()))
                                    .build();
                        }
                        return com.storechain.group4.dto.ShopCategoryDto.builder()
                                .id(existing.getId())
                                .name(existing.getName())
                                .sortOrder(existing.getSortOrder())
                                .totalProducts(existing.getTotalProducts() + count)
                                .imageUrl(existing.getImageUrl() != null
                                        ? existing.getImageUrl()
                                        : representativeImageUrl(c.getId()))
                                .build();
                    });
                });
        return new java.util.ArrayList<>(byName.values());
    }

    /** Ảnh đại diện danh mục: sản phẩm đại diện, không có thì sản phẩm mới nhất của danh mục */
    private String representativeImageUrl(Long categoryId) {
        return productRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId).stream()
                .filter(p -> p.getImageUrl() != null && !p.getImageUrl().isBlank())
                .filter(p -> p.isRepresentative())
                .findFirst()
                .or(() -> productRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId).stream()
                        .filter(p -> p.getImageUrl() != null && !p.getImageUrl().isBlank())
                        .findFirst())
                .map(p -> p.getImageUrl())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        ProductResponseDto dto = toCatalogDto(p);
        dto.setAvgRating(round1(reviewRepository.averageRatingByProductId(productId)));
        dto.setReviewCount(reviewRepository.countByProductId(productId));
        return dto;
    }

    private boolean sizeMatches(String sizes, String wanted) {
        if (sizes == null || sizes.isBlank()) return false;
        return Arrays.stream(sizes.split(","))
                .map(String::trim)
                .anyMatch(s -> s.equalsIgnoreCase(wanted));
    }

    /** DTO catalog: tồn kho = tổng của các chi nhánh, không gắn với chi nhánh cụ thể */
    private ProductResponseDto toCatalogDto(Product p) {
        return ProductResponseDto.builder()
                .id(p.getId())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .name(p.getName())
                .price(p.getPrice())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .stock(branchStockRepository.sumStockByProductId(p.getId()))
                .sizes(p.getSizes())
                .sizesList(parseSizes(p.getSizes()))
                .color(p.getColor())
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private List<String> parseSizes(String sizes) {
        if (sizes == null || sizes.isBlank()) return List.of();
        return Arrays.stream(sizes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private Double round1(Double v) {
        if (v == null) return 0.0;
        return BigDecimal.valueOf(v).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
