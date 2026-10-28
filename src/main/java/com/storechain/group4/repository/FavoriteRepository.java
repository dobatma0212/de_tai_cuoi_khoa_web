package com.storechain.group4.repository;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import com.storechain.group4.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /** Số lượt yêu thích của từng sản phẩm: mỗi dòng là [productId, count] */
    @Query("SELECT f.product.id, COUNT(f) FROM Favorite f GROUP BY f.product.id")
    List<Object[]> countGroupedByProduct();

    /** Xóa mọi lượt yêu thích của một sản phẩm (dùng khi xóa sản phẩm) */
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
