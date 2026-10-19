package com.storechain.group4.repository;

import com.storechain.group4.model.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    /** Top sản phẩm bán chạy (theo tổng quantity đã bán) */
    @Query("SELECT oi.product.id, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.status NOT IN ('CANCELLED', 'RETURNED') " +
           "GROUP BY oi.product.id ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProductIds(Pageable pageable);
}
