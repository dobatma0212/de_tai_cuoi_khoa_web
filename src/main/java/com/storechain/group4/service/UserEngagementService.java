package com.storechain.group4.service;

import com.storechain.group4.dto.FavoriteDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ProductViewDto;
import org.springframework.data.domain.Page;

/**
 * Tính năng tương tác người dùng: yêu thích, đã xem, top sản phẩm.
 */
public interface UserEngagementService {

    /** Toggle yêu thích. Trả về true nếu sau thao tác sản phẩm đang được yêu thích. */
    boolean toggleFavorite(Long userId, Long productId);

    boolean isFavorited(Long userId, Long productId);

    Page<FavoriteDto> getMyFavorites(Long userId, int page, int size);

    long countMyFavorites(Long userId);

    void recordProductView(Long userId, Long productId);

    Page<ProductViewDto> getMyViewedProducts(Long userId, int page, int size);

    /**
     * Top sản phẩm phân trang.
     * @param type newest | bestseller | top_rated | most_favorited
     */
    Page<ProductResponseDto> getTopProducts(String type, int page, int size);
}
