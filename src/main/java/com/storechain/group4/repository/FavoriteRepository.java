package com.storechain.group4.repository;

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
}
