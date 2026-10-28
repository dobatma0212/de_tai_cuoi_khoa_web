package com.storechain.group4.repository;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.Modifying;

import com.storechain.group4.model.ProductView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    Optional<ProductView> findByUserIdAndProductId(Long userId, Long productId);

    List<ProductView> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);

    List<ProductView> findByUserId(Long userId);

    /** Xóa mọi lượt xem của một sản phẩm (dùng khi xóa sản phẩm) */
    @Modifying
    @Query("DELETE FROM ProductView v WHERE v.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
