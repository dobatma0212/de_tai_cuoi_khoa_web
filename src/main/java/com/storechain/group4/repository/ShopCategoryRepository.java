package com.storechain.group4.repository;

import com.storechain.group4.model.ShopCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopCategoryRepository extends JpaRepository<ShopCategory, Long> {

    List<ShopCategory> findByShopIdOrderBySortOrderAscIdAsc(Long shopId);

    long countByShopId(Long shopId);

    boolean existsByShopIdAndNameIgnoreCase(Long shopId, String name);

    boolean existsByShopIdAndNameIgnoreCaseAndIdNot(Long shopId, String name, Long id);

    Optional<ShopCategory> findByIdAndShopId(Long id, Long shopId);
}
