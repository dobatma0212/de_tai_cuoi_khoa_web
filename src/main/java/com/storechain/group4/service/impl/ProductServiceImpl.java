package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.model.Category;
import com.storechain.group4.model.Product;
import com.storechain.group4.repository.CategoryRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto.PageDto getProducts(String keyword, String categorySlug, String sort, Pageable pageable) {
        List<Long> categoryIds = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục: " + categorySlug));
            if (!category.isActive()) {
                throw new IllegalArgumentException("Danh mục không hoạt động: " + categorySlug);
            }
            categoryIds = collectCategoryIds(category);
        }

        String kw = keyword == null ? "" : keyword.trim();
        Pageable sorted = applySort(pageable, sort);
        Page<Product> page = (categoryIds == null)
                ? productRepository.searchProducts(kw, sorted)
                : productRepository.searchProductsInCategories(kw, categoryIds, sorted);

        return toPageDto(page, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + slug));
        if (!product.isActive()) {
            throw new IllegalArgumentException("Sản phẩm không còn kinh doanh: " + slug);
        }
        return toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getRelatedProducts(String slug, int limit) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + slug));
        List<Long> categoryIds = collectCategoryIds(product.getCategory());
        Page<Product> related = productRepository.findRelatedProducts(
                categoryIds, product.getId(), PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "soldCount"))
        );
        return related.getContent().stream().map(this::toDto).toList();
    }

    private Pageable applySort(Pageable pageable, String sort) {
        Sort jpaSort = switch (sort == null ? "" : sort.toLowerCase()) {
            case "bestselling" -> Sort.by(Sort.Direction.DESC, "soldCount");
            case "rating" -> Sort.by(Sort.Direction.DESC, "ratingAvg");
            case "favorite" -> Sort.by(Sort.Direction.DESC, "favoriteCount");
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), jpaSort);
    }

    private List<Long> collectCategoryIds(Category root) {
        List<Long> ids = new ArrayList<>();
        ids.add(root.getId());
        List<Category> children = categoryRepository.findByParentIdOrderBySortOrderAsc(root.getId());
        for (Category child : children) {
            ids.add(child.getId());
            ids.addAll(categoryRepository.findByParentIdOrderBySortOrderAsc(child.getId())
                    .stream().map(Category::getCategoryId).toList());
        }
        return ids;
    }

    private ProductResponseDto.PageDto toPageDto(Page<Product> page, Pageable pageable) {
        return ProductResponseDto.PageDto.builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private ProductResponseDto toDto(Product p) {
        return ProductResponseDto.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .price(p.getPrice())
                .originalPrice(p.getOriginalPrice())
                .imageUrl(p.getImageUrl())
                .categoryId(p.getCategory().getCategoryId())
                .categoryName(p.getCategory().getName())
                .categorySlug(p.getCategory().getSlug())
                .stock(p.getStock())
                .soldCount(p.getSoldCount())
                .ratingAvg(p.getRatingAvg())
                .reviewCount(p.getReviewCount())
                .favoriteCount(p.getFavoriteCount())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
