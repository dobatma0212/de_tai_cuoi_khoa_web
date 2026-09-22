package com.storechain.group4.repository;

import com.storechain.group4.model.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    @Query(value = "SELECT s FROM Shop s LEFT JOIN FETCH s.manager m WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR :status = '' OR s.status = :status)",
            countQuery = "SELECT COUNT(s) FROM Shop s LEFT JOIN s.manager m WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR :status = '' OR s.status = :status)")
    Page<Shop> searchShops(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    long countByStatus(String status);

    long countByManagerIsNotNull();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Shop> findByManagerId(Long managerId);

    List<Shop> findByStatus(String status);
}
