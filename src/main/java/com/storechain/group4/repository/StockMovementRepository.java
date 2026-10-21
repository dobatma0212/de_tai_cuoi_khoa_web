package com.storechain.group4.repository;

import com.storechain.group4.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<StockMovement> findByRefTypeAndRefId(String refType, Long refId);
}
