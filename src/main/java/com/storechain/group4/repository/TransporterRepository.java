package com.storechain.group4.repository;

import com.storechain.group4.model.Transporter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface TransporterRepository extends JpaRepository<Transporter, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Transporter> findByEmail(String email);

    long countByStatus(String status);

    @Query("SELECT t FROM Transporter t WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(t.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR LOWER(t.status) = LOWER(:status))")
    Page<Transporter> searchTransporters(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT COALESCE(AVG(t.baseFee), 0) FROM Transporter t")
    BigDecimal averageBaseFee();
}
