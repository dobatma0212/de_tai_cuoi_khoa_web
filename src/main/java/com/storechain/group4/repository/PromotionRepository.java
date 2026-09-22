package com.storechain.group4.repository;

import com.storechain.group4.model.Promotion;
import com.storechain.group4.model.PromotionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // ===== Khuyến mãi theo chi nhánh (manager) =====

    List<Promotion> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<Promotion> findByIdAndShopId(Long id, Long shopId);

    /** Tìm khuyến mãi đang bật theo mã trong phạm vi một chi nhánh */
    @Query("SELECT p FROM Promotion p WHERE p.shop.id = :shopId AND LOWER(p.code) = LOWER(:code) AND p.status = 'active'")
    Optional<Promotion> findActiveByShopIdAndCodeIgnoreCase(@Param("shopId") Long shopId, @Param("code") String code);

    boolean existsByShopIdAndCodeIgnoreCaseAndIdNot(Long shopId, String code, Long id);

    // ===== Khuyến mãi toàn server (admin) =====

    // Search with pagination - JPQL query filtering by keyword (matches name or code), status, and promotionType
    @Query("SELECT p FROM Promotion p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (p.code IS NOT NULL AND LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "AND (:status IS NULL OR :status = '' OR LOWER(p.status) = LOWER(:status)) " +
           "AND (:promotionType IS NULL OR p.promotionType = :promotionType)")
    Page<Promotion> searchPromotions(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("promotionType") PromotionType promotionType,
            Pageable pageable
    );

    // Find by code (case insensitive), chỉ trong các khuyến mãi toàn server (shop IS NULL)
    Optional<Promotion> findByCodeIgnoreCaseAndShopIsNull(String code);

    // Check code existence (phạm vi toàn server)
    boolean existsByCodeIgnoreCaseAndShopIsNull(String code);

    boolean existsByCodeIgnoreCaseAndShopIsNullAndIdNot(String code, Long id);

    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    // Stats
    long countByStatus(String status);

    long countByPromotionType(PromotionType promotionType);

    long countByPromotionTypeAndStatus(PromotionType promotionType, String status);

    // PESSIMISTIC WRITE lock for race condition
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") Long id);

    // Atomic increment
    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1 WHERE p.id = :id AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)")
    int incrementUsedCount(@Param("id") Long id);

    // Auto-expire
    @Modifying
    @Query("UPDATE Promotion p SET p.status = 'expired' WHERE p.endDate < :now AND p.status = 'active'")
    int expirePromotions(@Param("now") LocalDateTime now);

    // Find active flash sales
    @Query("SELECT p FROM Promotion p WHERE p.promotionType = com.storechain.group4.model.PromotionType.FLASH_SALE AND p.status = 'active' AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActiveFlashSales(@Param("now") LocalDateTime now);

    default List<Promotion> findActiveFlashSales(PromotionType promotionType, String status, LocalDateTime now) {
        return findActiveFlashSales(now);
    }

    // Find active flash sales for specific product
    @Query("SELECT p FROM Promotion p JOIN p.products prod WHERE prod.id = :productId AND p.promotionType = com.storechain.group4.model.PromotionType.FLASH_SALE AND p.status = 'active' AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActiveFlashSalesForProduct(@Param("productId") Long productId, @Param("now") LocalDateTime now);
}
