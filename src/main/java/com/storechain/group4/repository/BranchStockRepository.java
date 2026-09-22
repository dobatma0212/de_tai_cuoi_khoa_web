package com.storechain.group4.repository;

import com.storechain.group4.model.BranchStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {

    Optional<BranchStock> findByProductIdAndShopId(Long productId, Long shopId);

    /** Tổng tồn kho của một sản phẩm trên toàn bộ chi nhánh */
    @Query("SELECT COALESCE(SUM(bs.stock), 0) FROM BranchStock bs WHERE bs.product.id = :productId")
    int sumStockByProductId(@Param("productId") Long productId);
}
