package com.storechain.group4.repository;

import com.storechain.group4.model.ReceiptStatus;
import com.storechain.group4.model.StockReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {

    List<StockReceipt> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<StockReceipt> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, ReceiptStatus status);

    boolean existsByReceiptCode(String receiptCode);

    /** Đếm số phiếu tạo trong ngày (để sinh mã PN-yyyyMMdd-####) */
    @Query("SELECT COUNT(r) FROM StockReceipt r WHERE r.createdAt >= :start AND r.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
