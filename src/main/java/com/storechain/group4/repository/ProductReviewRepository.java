package com.storechain.group4.repository;

import com.storechain.group4.model.ProductReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    long countByProductId(Long productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r WHERE r.product.id = :productId")
    Double averageRatingByProductId(@Param("productId") Long productId);

    /** Top sản phẩm đánh giá cao nhất (có ít nhất 1 review) */
    @Query("SELECT r.product.id, AVG(r.rating) as avgRating, COUNT(r) as cnt " +
           "FROM ProductReview r GROUP BY r.product.id " +
           "HAVING COUNT(r) >= 1 ORDER BY avgRating DESC, cnt DESC")
    List<Object[]> findTopRatedProductIds(Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
