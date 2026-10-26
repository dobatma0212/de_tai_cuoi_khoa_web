package com.storechain.group4.repository;

import com.storechain.group4.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<Order> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, String status);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByStatus(String status);

    /** Các đơn do một nhân viên phụ trách (tính hoa hồng) */
    List<Order> findByHandledByIdOrderByCreatedAtDesc(Long employeeId);

    boolean existsByOrderCode(String orderCode);

    /** Các đơn cùng một lần thanh toán online */
    List<Order> findByPaymentRef(String paymentRef);

    /** Đơn thanh toán online chưa được cổng xác nhận (dùng để hết hạn tự hủy) */
    List<Order> findByPaymentStatusAndStatusAndCreatedAtBefore(String paymentStatus, String status, LocalDateTime before);

    /** Doanh thu tháng của chi nhánh (các đơn đã giao/nhận hoặc đã thanh toán thành công) */
    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.shop.id = :shopId " +
           "AND (o.status IN ('DELIVERED', 'RECEIVED') OR (o.paymentStatus = 'PAID' AND o.status NOT IN ('CANCELLED', 'RETURNED'))) " +
           "AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    BigDecimal sumMonthlyRevenueByShopId(
            @Param("shopId") Long shopId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /** Số đơn hàng hoàn tất/thanh toán trong tháng của chi nhánh */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.id = :shopId " +
           "AND (o.status IN ('DELIVERED', 'RECEIVED') OR (o.paymentStatus = 'PAID' AND o.status NOT IN ('CANCELLED', 'RETURNED'))) " +
           "AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    long countMonthlyOrdersByShopId(
            @Param("shopId") Long shopId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /** Tổng doanh thu lũy kế của chi nhánh */
    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.shop.id = :shopId " +
           "AND (o.status IN ('DELIVERED', 'RECEIVED') OR (o.paymentStatus = 'PAID' AND o.status NOT IN ('CANCELLED', 'RETURNED')))")
    BigDecimal sumTotalRevenueByShopId(@Param("shopId") Long shopId);

    /** Tổng số đơn hàng của chi nhánh */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.id = :shopId")
    long countTotalOrdersByShopId(@Param("shopId") Long shopId);

    /** Danh sách đơn hàng gần đây của chi nhánh kèm thông tin khách hàng */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.shop.id = :shopId ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByShopId(@Param("shopId") Long shopId, Pageable pageable);
}
