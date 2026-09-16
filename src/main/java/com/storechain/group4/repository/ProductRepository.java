package com.storechain.group4.repository;

import com.storechain.group4.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<Product> findByShopIdAndCategoryIdOrderByCreatedAtDesc(Long shopId, Long categoryId);

    List<Product> findByShopIdAndCategoryIdAndRepresentativeTrue(Long shopId, Long categoryId);

    boolean existsByShopIdAndCategoryIdAndRepresentativeTrue(Long shopId, Long categoryId);

    long countByShopIdAndCategoryId(Long shopId, Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.status = 'active' AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("shopId") Long shopId,
                                 @Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.status = 'active' AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> suggestByName(@Param("shopId") Long shopId, @Param("keyword") String keyword);
}
