package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ProductPageDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Promotion;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.ShopCategoryRepository;
import com.storechain.group4.service.CatalogService;
import com.storechain.group4.service.PromotionCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final com.storechain.group4.repository.OrderItemRepository orderItemRepository;
    private final com.storechain.group4.repository.FavoriteRepository favoriteRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionCalculator promotionCalculator;

    @Override
    @Transactional(readOnly = true)
    public ProductPageDto searchProducts(String keyword, Long categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String size, String color, String sort, int page, int pageSize) {
        String kw = keyword == null ? "" : keyword.trim();
        List<Product> products = productRepository.searchAllProducts(kw);
        Stats stats = loadStats();
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());

        // Lọc trong bộ nhớ (khối lượng dữ liệu demo nhỏ nên chấp nhận được)
        List<Product> filtered = products.stream()
                .filter(p -> categoryId == null || categoryId <= 0
                        || (p.getCategory() != null && p.getCategory().getId().equals(categoryId)))
                .filter(p -> minPrice == null
                        || promotionCalculator.resolveEffectivePrice(activeFlashSales, p).compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null
                        || promotionCalculator.resolveEffectivePrice(activeFlashSales, p).compareTo(maxPrice) <= 0)
                .filter(p -> size == null || size.isBlank()
                        || sizeMatches(p.getSizes(), size.trim()))
                .filter(p -> color == null || color.isBlank()
                        || (p.getColor() != null && p.getColor().toLowerCase(java.util.Locale.ROOT)
                                .contains(color.trim().toLowerCase(java.util.Locale.ROOT))))
                .collect(Collectors.toList());

        if ("priceAsc".equals(sort)) {
            filtered.sort((a, b) -> promotionCalculator.resolveEffectivePrice(activeFlashSales, a)
                    .compareTo(promotionCalculator.resolveEffectivePrice(activeFlashSales, b)));
        } else if ("priceDesc".equals(sort)) {
            filtered.sort((a, b) -> promotionCalculator.resolveEffectivePrice(activeFlashSales, b)
                    .compareTo(promotionCalculator.resolveEffectivePrice(activeFlashSales, a)));
        } else if ("name".equals(sort)) {
            filtered.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        } else if ("bestseller".equals(sort)) {
            filtered.sort(bySold(stats).thenComparing(newestFirst()));
        } else if ("rating".equals(sort)) {
            filtered.sort(byRating(stats).thenComparing(newestFirst()));
        } else if ("favorite".equals(sort)) {
            filtered.sort(byFavorite(stats).thenComparing(newestFirst()));
        } else {
            filtered.sort(newestFirst()); // mặc định: mới nhất
        }

        int safePageSize = pageSize <= 0 ? 12 : pageSize;
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) filtered.size() / safePageSize);
        int from = Math.min(safePage * safePageSize, filtered.size());
        int to = Math.min(from + safePageSize, filtered.size());

        List<ProductResponseDto> content = filtered.subList(from, to)
                .stream().map(p -> toCatalogDto(p, stats, activeFlashSales)).collect(Collectors.toList());

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
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());
        return productRepository.suggestAllByName(keyword.trim())
                .stream().limit(8).map(p -> toCatalogDto(p, null, activeFlashSales)).toList();
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
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());
        ProductResponseDto dto = toCatalogDto(p, null, activeFlashSales);
        dto.setAvgRating(round1(reviewRepository.averageRatingByProductId(productId)));
        dto.setReviewCount(reviewRepository.countByProductId(productId));
        return dto;
    }

    // ============ Top 20 cho trang chủ ============

    private static final int HOME_LIST_LIMIT = 20;

    @Override
    @Transactional(readOnly = true)
    public ProductPageDto topProducts(String type, int page, int pageSize) {
        String t = type == null ? "new" : type.trim().toLowerCase(java.util.Locale.ROOT);
        Stats stats = loadStats();
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());
        List<Product> all = new java.util.ArrayList<>(productRepository.searchAllProducts(""));

        switch (t) {
            case "bestseller" -> all.sort(bySold(stats).thenComparing(newestFirst()));
            case "toprated" -> {
                all.removeIf(p -> stats.ratingCount.getOrDefault(p.getId(), 0L) == 0L);
                all.sort(byRating(stats).thenComparing(newestFirst()));
            }
            case "favorite" -> {
                all.removeIf(p -> stats.favorites.getOrDefault(p.getId(), 0L) == 0L);
                all.sort(byFavorite(stats).thenComparing(newestFirst()));
            }
            default -> all.sort(newestFirst());
        }

        List<Product> top = all.size() > HOME_LIST_LIMIT ? all.subList(0, HOME_LIST_LIMIT) : all;
        int safePageSize = pageSize <= 0 ? 8 : Math.min(pageSize, HOME_LIST_LIMIT);
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) top.size() / safePageSize);
        int from = Math.min(safePage * safePageSize, top.size());
        int to = Math.min(from + safePageSize, top.size());

        List<ProductResponseDto> content = top.subList(from, to).stream()
                .map(p -> toCatalogDto(p, stats, activeFlashSales)).collect(Collectors.toList());
        return ProductPageDto.builder()
                .content(content)
                .page(safePage)
                .pageSize(safePageSize)
                .totalElements(top.size())
                .totalPages(totalPages)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> toCards(List<Product> products) {
        Stats stats = loadStats();
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());
        return products.stream().map(p -> toCatalogDto(p, stats, activeFlashSales)).collect(Collectors.toList());
    }

    /** Số liệu thống kê gộp theo sản phẩm, nạp một lần cho cả danh sách (tránh N+1) */
    private static final class Stats {
        final java.util.Map<Long, Long> sold = new java.util.HashMap<>();
        final java.util.Map<Long, Double> avgRating = new java.util.HashMap<>();
        final java.util.Map<Long, Long> ratingCount = new java.util.HashMap<>();
        final java.util.Map<Long, Long> favorites = new java.util.HashMap<>();
    }

    private Stats loadStats() {
        Stats s = new Stats();
        for (Object[] row : orderItemRepository.sumSoldGroupedByProduct()) {
            s.sold.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        for (Object[] row : reviewRepository.ratingStatsGroupedByProduct()) {
            Long id = ((Number) row[0]).longValue();
            s.avgRating.put(id, row[1] == null ? 0.0 : ((Number) row[1]).doubleValue());
            s.ratingCount.put(id, ((Number) row[2]).longValue());
        }
        for (Object[] row : favoriteRepository.countGroupedByProduct()) {
            s.favorites.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return s;
    }

    private java.util.Comparator<Product> newestFirst() {
        return java.util.Comparator.comparing(Product::getCreatedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.<java.time.LocalDateTime>reverseOrder()));
    }

    private java.util.Comparator<Product> bySold(Stats s) {
        return (a, b) -> Long.compare(s.sold.getOrDefault(b.getId(), 0L), s.sold.getOrDefault(a.getId(), 0L));
    }

    private java.util.Comparator<Product> byRating(Stats s) {
        return (a, b) -> {
            int c = Double.compare(s.avgRating.getOrDefault(b.getId(), 0.0), s.avgRating.getOrDefault(a.getId(), 0.0));
            return c != 0 ? c : Long.compare(s.ratingCount.getOrDefault(b.getId(), 0L), s.ratingCount.getOrDefault(a.getId(), 0L));
        };
    }

    private java.util.Comparator<Product> byFavorite(Stats s) {
        return (a, b) -> Long.compare(s.favorites.getOrDefault(b.getId(), 0L), s.favorites.getOrDefault(a.getId(), 0L));
    }

    private boolean sizeMatches(String sizes, String wanted) {
        if (sizes == null || sizes.isBlank()) return false;
        return Arrays.stream(sizes.split(","))
                .map(String::trim)
                .anyMatch(s -> s.equalsIgnoreCase(wanted));
    }

    /** DTO catalog: tồn kho = tổng của các chi nhánh, không gắn với chi nhánh cụ thể */
    private ProductResponseDto toCatalogDto(Product p, Stats stats, List<Promotion> activeFlashSales) {
        ProductResponseDto dto = ProductResponseDto.builder()
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
        promotionCalculator.applyFlashSaleToDto(dto, p.getPrice(), activeFlashSales);
        if (stats != null) {
            dto.setSoldCount(stats.sold.getOrDefault(p.getId(), 0L));
            dto.setFavoriteCount(stats.favorites.getOrDefault(p.getId(), 0L));
            dto.setReviewCount(stats.ratingCount.getOrDefault(p.getId(), 0L));
            dto.setAvgRating(round1(stats.avgRating.getOrDefault(p.getId(), 0.0)));
        }
        return dto;
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
