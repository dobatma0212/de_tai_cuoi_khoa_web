package com.storechain.group4.repository;

import com.storechain.group4.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.status) = 'active' " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.status) = 'active' " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND p.category.id IN :categoryIds")
    Page<Product> searchProductsInCategories(
            @Param("keyword") String keyword,
            @Param("categoryIds") List<Long> categoryIds,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.status) = 'active' " +
            "AND p.category.id IN :categoryIds " +
            "AND p.id <> :excludeId")
    Page<Product> findRelatedProducts(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("excludeId") Long excludeId,
            Pageable pageable
    );

    long countByStatus(String status);
}
