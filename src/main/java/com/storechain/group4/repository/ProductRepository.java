package com.storechain.group4.repository;

import com.storechain.group4.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByShopId(Long shopId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR :status = '' OR LOWER(p.status) = LOWER(:status)) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> searchProducts(
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND LOWER(p.status) = 'active' AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchPublicProducts(
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
