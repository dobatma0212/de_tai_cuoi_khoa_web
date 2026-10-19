package com.storechain.group4.service.impl;

import com.storechain.group4.dto.FavoriteDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ProductViewDto;
import com.storechain.group4.model.Favorite;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.ProductView;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.FavoriteRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.ProductViewRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.UserEngagementService;
import com.storechain.group4.service.support.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEngagementServiceImpl implements UserEngagementService {

    private final FavoriteRepository favoriteRepository;
    private final ProductViewRepository productViewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductReviewRepository productReviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;

    // ==================== Favorite ====================

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long productId) {
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        Favorite fav = Favorite.builder().user(user).product(product).build();
        favoriteRepository.save(fav);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, Long productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteDto> getMyFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Favorite> pageData = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return pageData.map(this::toFavoriteDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyFavorites(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

    private FavoriteDto toFavoriteDto(Favorite f) {
        Product p = f.getProduct();
        return FavoriteDto.builder()
                .id(f.getId())
                .productId(p.getId())
                .productName(p.getName())
                .productImage(p.getImageUrl())
                .productPrice(p.getPrice())
                .productSlug(p.getSlug())
                .inStock(p.getStock() > 0)
                .createdAt(f.getCreatedAt())
                .build();
    }

    // ==================== Product View ====================

    @Override
    @Transactional
    public void recordProductView(Long userId, Long productId) {
        Optional<ProductView> existing = productViewRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            ProductView pv = existing.get();
            pv.setViewCount(pv.getViewCount() + 1);
            productViewRepository.save(pv);
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        ProductView pv = ProductView.builder()
                .user(user)
                .product(product)
                .viewCount(1)
                .build();
        productViewRepository.save(pv);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductViewDto> getMyViewedProducts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ProductView> pageData = productViewRepository.findByUserIdOrderByLastViewedAtDesc(userId, pageable);
        return pageData.map(this::toViewDto);
    }

    private ProductViewDto toViewDto(ProductView pv) {
        Product p = pv.getProduct();
        return ProductViewDto.builder()
                .id(pv.getId())
                .productId(p.getId())
                .productName(p.getName())
                .productImage(p.getImageUrl())
                .productPrice(p.getPrice())
                .productSlug(p.getSlug())
                .viewCount(pv.getViewCount())
                .lastViewedAt(pv.getLastViewedAt())
                .build();
    }

    // ==================== Top Products ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getTopProducts(String type, int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), 20);
        Pageable pageable = PageRequest.of(page, pageSize);
        String t = type == null ? "newest" : type.trim().toLowerCase();

        return switch (t) {
            case "bestseller" -> buildFromIdRows(
                    orderItemRepository.findTopSellingProductIds(PageRequest.of(0, pageSize * (page + 1))),
                    page, pageSize, pageable);
            case "top_rated" -> buildFromIdRows(
                    productReviewRepository.findTopRatedProductIds(PageRequest.of(0, pageSize * (page + 1))),
                    page, pageSize, pageable);
            case "most_favorited" -> buildFromIdRows(
                    favoriteRepository.findTopFavoritedProductIds(PageRequest.of(0, pageSize * (page + 1))),
                    page, pageSize, pageable);
            default -> { // newest
                Pageable newestPage = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
                yield productRepository.findAll(newestPage).map(p -> productMapper.toDto(p, null));
            }
        };
    }

    private Page<ProductResponseDto> buildFromIdRows(List<Object[]> rows, int page, int pageSize, Pageable pageable) {
        List<Long> ids = rows.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, rows.size());
        }
        List<Product> list = productRepository.findAllById(ids);
        List<Product> ordered = new ArrayList<>();
        for (Long id : ids) {
            list.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(ordered::add);
        }
        List<ProductResponseDto> dtos = ordered.stream()
                .map(p -> productMapper.toDto(p, null))
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, rows.size());
    }
}

