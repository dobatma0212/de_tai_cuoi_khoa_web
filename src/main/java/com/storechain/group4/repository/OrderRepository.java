package com.storechain.group4.repository;

import com.storechain.group4.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
