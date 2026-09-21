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
}
