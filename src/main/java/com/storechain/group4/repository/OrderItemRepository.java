package com.storechain.group4.repository;

import com.storechain.group4.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    /** Số lượng đã bán của từng sản phẩm (bỏ đơn hủy / trả hàng): mỗi dòng là [productId, tổng số lượng] */
    @Query("SELECT oi.product.id, COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.product IS NOT NULL AND oi.order.status NOT IN ('CANCELLED', 'RETURNED') " +
           "GROUP BY oi.product.id")
    List<Object[]> sumSoldGroupedByProduct();

    /** Số dòng đơn hàng đã giao/nhận của user cho một sản phẩm (>0 nghĩa là đã mua) */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.user.id = :userId " +
           "AND oi.product.id = :productId AND oi.order.status IN ('DELIVERED', 'RECEIVED')")
    long countPurchasedByUser(@Param("userId") Long userId, @Param("productId") Long productId);
}
