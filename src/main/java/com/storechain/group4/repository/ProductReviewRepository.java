package com.storechain.group4.repository;

import com.storechain.group4.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    long countByProductId(Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    /** Điểm trung bình + số đánh giá của từng sản phẩm: mỗi dòng là [productId, avg, count] */
    @Query("SELECT r.product.id, AVG(r.rating), COUNT(r) FROM ProductReview r GROUP BY r.product.id")
    List<Object[]> ratingStatsGroupedByProduct();

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r WHERE r.product.id = :productId")
    Double averageRatingByProductId(@Param("productId") Long productId);
}
