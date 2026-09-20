package com.storechain.group4.repository;

import com.storechain.group4.model.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    long countByPromotionIdAndUserId(Long promotionId, Long userId);
    boolean existsByPromotionIdAndUserId(Long promotionId, Long userId);
    void deleteByPromotionId(Long promotionId);
}
