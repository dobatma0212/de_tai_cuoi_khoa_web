package com.storechain.group4.repository;

import com.storechain.group4.model.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByProductId(Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    /** Top sản phẩm được yêu thích nhiều nhất */
    @Query("SELECT f.product.id, COUNT(f) as cnt FROM Favorite f GROUP BY f.product.id ORDER BY cnt DESC")
    List<Object[]> findTopFavoritedProductIds(Pageable pageable);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.product.id = :productId")
    long countFavoritesByProductId(@Param("productId") Long productId);
}
