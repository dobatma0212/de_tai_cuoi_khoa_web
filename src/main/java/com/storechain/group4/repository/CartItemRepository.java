package com.storechain.group4.repository;

import com.storechain.group4.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndProductIdAndSizeAndColor(Long userId, Long productId,
                                                               String size, String color);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Transactional
    void deleteByUserId(Long userId);
}
