package com.storechain.group4.repository;

import com.storechain.group4.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<Promotion> findByIdAndShopId(Long id, Long shopId);

    Optional<Promotion> findByShopIdAndCodeIgnoreCaseAndActiveTrue(Long shopId, String code);
}
