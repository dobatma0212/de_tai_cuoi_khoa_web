package com.storechain.group4.repository;

import com.storechain.group4.model.BranchStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {

    Optional<BranchStock> findByProductIdAndShopId(Long productId, Long shopId);

    List<BranchStock> findByProductId(Long productId);

    List<BranchStock> findByShopId(Long shopId);

    Optional<BranchStock> findByShopIdAndProductId(Long shopId, Long productId);

    long countByShopId(Long shopId);

    long countByProductId(Long productId);

    /** Tổng tồn kho toàn hệ thống của một sản phẩm (mọi chi nhánh) */
    @Query("SELECT COALESCE(SUM(b.stock), 0) FROM BranchStock b WHERE b.product.id = :productId")
    int sumStockByProductId(@Param("productId") Long productId);

    /** Tổng tồn kho của một chi nhánh (mọi sản phẩm) */
    @Query("SELECT COALESCE(SUM(b.stock), 0) FROM BranchStock b WHERE b.shop.id = :shopId")
    int sumStockByShopId(@Param("shopId") Long shopId);

    /** Danh sách tồn kho sản phẩm tại chi nhánh kèm thông tin sản phẩm và danh mục */
    @Query("SELECT b FROM BranchStock b JOIN FETCH b.product p LEFT JOIN FETCH p.category WHERE b.shop.id = :shopId ORDER BY b.stock DESC, p.id DESC")
    List<BranchStock> findByShopIdWithProduct(@Param("shopId") Long shopId);

    void deleteByShopId(Long shopId);
}
