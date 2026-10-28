package com.storechain.group4.repository;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.Modifying;

import com.storechain.group4.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<StockMovement> findByRefTypeAndRefId(String refType, Long refId);

    /** Gỡ liên kết sản phẩm khỏi lịch sử kho (giữ snapshot tên) khi xóa sản phẩm */
    @Modifying
    @Query("UPDATE StockMovement m SET m.product = null WHERE m.product.id = :productId")
    void detachProduct(@Param("productId") Long productId);
}
