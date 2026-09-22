package com.storechain.group4.repository;

import com.storechain.group4.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** Sản phẩm đang bán, mới nhất trước (dùng cho trang chủ / cửa hàng) */
    @Query("SELECT p FROM Product p WHERE LOWER(p.status) = 'active' ORDER BY p.createdAt DESC, p.id DESC")
    List<Product> findActiveProducts();

    @Query("SELECT p FROM Product p WHERE LOWER(p.status) = 'active' AND p.category.id = :categoryId "
            + "ORDER BY p.createdAt DESC, p.id DESC")
    List<Product> findActiveProductsByCategory(@Param("categoryId") Long categoryId);

    /** Sản phẩm đại diện hiển thị nổi bật ở trang chủ */
    @Query("SELECT p FROM Product p WHERE LOWER(p.status) = 'active' AND p.representative = true "
            + "ORDER BY p.createdAt DESC, p.id DESC")
    List<Product> findActiveRepresentativeProducts();
}
