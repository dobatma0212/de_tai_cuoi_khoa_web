package com.storechain.group4.repository;

import com.storechain.group4.model.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {

    List<ShippingAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<ShippingAddress> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE ShippingAddress a SET a.isDefault = false WHERE a.user.id = :userId AND a.isDefault = true")
    void clearDefaultByUserId(@Param("userId") Long userId);
}
