package com.storechain.group4.repository;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.Modifying;

import com.storechain.group4.model.StockReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReceiptItemRepository extends JpaRepository<StockReceiptItem, Long> {

    List<StockReceiptItem> findByReceiptId(Long receiptId);

    /** Gỡ liên kết sản phẩm khỏi phiếu nhập (giữ snapshot tên) khi xóa sản phẩm */
    @Modifying
    @Query("UPDATE StockReceiptItem i SET i.product = null WHERE i.product.id = :productId")
    void detachProduct(@Param("productId") Long productId);
}
