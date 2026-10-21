package com.storechain.group4.service;

import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.model.Favorite;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.ProductView;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.FavoriteRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductViewRepository;
import com.storechain.group4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hoạt động của người mua: sản phẩm yêu thích và sản phẩm đã xem.
 */
@Service
@RequiredArgsConstructor
public class ShopperActivityService {

    /** Chỉ giữ tối đa chừng này sản phẩm đã xem gần nhất cho mỗi người dùng */
    private static final int MAX_VIEW_HISTORY = 50;

    private final FavoriteRepository favoriteRepository;
    private final ProductViewRepository productViewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;

    // ============ Yêu thích ============

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getFavorites(Long userId) {
        List<Product> products = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(Favorite::getProduct).toList();
        return catalogService.toCards(products);
    }

    @Transactional(readOnly = true)
    public List<Long> getFavoriteIds(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(f -> f.getProduct().getId()).toList();
    }

    /** Thêm vào yêu thích (idempotent: đã thích rồi thì giữ nguyên) */
    @Transactional
    public void addFavorite(Long userId, Long productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        favoriteRepository.save(Favorite.builder().user(user).product(product).build());
    }

    /** Bỏ yêu thích (idempotent) */
    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        favoriteRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(favoriteRepository::delete);
    }

    // ============ Sản phẩm đã xem ============

    /** Ghi nhận đã xem; xem lại thì chỉ cập nhật thời điểm, đồng thời cắt bớt lịch sử quá dài */
    @Transactional
    public void recordView(Long userId, Long productId) {
        ProductView existing = productViewRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        if (existing != null) {
            existing.setViewedAt(LocalDateTime.now());
            productViewRepository.save(existing);
            return;
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        productViewRepository.save(ProductView.builder().user(user).product(product).build());

        List<ProductView> all = productViewRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, MAX_VIEW_HISTORY + 20));
        if (all.size() > MAX_VIEW_HISTORY) {
            productViewRepository.deleteAll(all.subList(MAX_VIEW_HISTORY, all.size()));
        }
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getRecentlyViewed(Long userId, int limit) {
        int safe = Math.min(Math.max(limit, 1), MAX_VIEW_HISTORY);
        List<Product> products = productViewRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, safe))
                .stream().map(ProductView::getProduct).toList();
        return catalogService.toCards(products);
    }

    @Transactional
    public void clearViewHistory(Long userId) {
        productViewRepository.deleteAll(productViewRepository.findByUserId(userId));
    }
}
