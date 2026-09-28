package com.storechain.group4.repository;

import com.storechain.group4.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<Product> findByShopIdAndCategoryIdOrderByCreatedAtDesc(Long shopId, Long categoryId);

    List<Product> findByShopIdAndCategoryIdAndRepresentativeTrue(Long shopId, Long categoryId);

    boolean existsByShopIdAndCategoryIdAndRepresentativeTrue(Long shopId, Long categoryId);

    long countByShopIdAndCategoryId(Long shopId, Long categoryId);

    long countByCategoryId(Long categoryId);

    List<Product> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.status = 'active' AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("shopId") Long shopId,
                                 @Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.status = 'active' AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchAllProducts(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.status = 'active' AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> suggestAllByName(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.status = 'active' AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> suggestByName(@Param("shopId") Long shopId, @Param("keyword") String keyword);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query(value = "SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH p.category c WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.slug, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:shopId IS NULL OR p.shop.id = :shopId) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:status IS NULL OR :status = '' OR p.status = :status) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:inStock IS NULL OR (:inStock = true AND p.stock > 0) OR (:inStock = false AND p.stock <= 0))",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.slug, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:shopId IS NULL OR p.shop.id = :shopId) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:status IS NULL OR :status = '' OR p.status = :status) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:inStock IS NULL OR (:inStock = true AND p.stock > 0) OR (:inStock = false AND p.stock <= 0))")
    Page<Product> searchProductsAdmin(
            @Param("keyword") String keyword,
            @Param("shopId") Long shopId,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStock") Boolean inStock,
            Pageable pageable
    );

    long countByStatus(String status);

    long countByStockLessThanEqual(int stock);

    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p")
    long sumAllStock();

    @Query("SELECT COUNT(DISTINCT p.shop.id) FROM Product p")
    long countDistinctShops();

    @Query("SELECT COUNT(DISTINCT p.category.id) FROM Product p")
    long countDistinctCategories();
}
