package com.storechain.group4.repository;

import com.storechain.group4.model.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findAllByOwnerIdOrderByIdDesc(Long ownerId);

    List<Shop> findAllByOwnerIdAndStatusIgnoreCaseOrderByIdDesc(Long ownerId, String status);

    Optional<Shop> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndStatusIgnoreCase(Long ownerId, String status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<Shop> findByStatusIgnoreCase(String status, Pageable pageable);

    @Query("SELECT s FROM Shop s WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.owner.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.owner.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR LOWER(s.status) = LOWER(:status))")
    Page<Shop> searchShops(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
