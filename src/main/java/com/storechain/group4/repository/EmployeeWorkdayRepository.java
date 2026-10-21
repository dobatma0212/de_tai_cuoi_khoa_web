package com.storechain.group4.repository;

import com.storechain.group4.model.EmployeeWorkday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeWorkdayRepository extends JpaRepository<EmployeeWorkday, Long> {

    Optional<EmployeeWorkday> findByUserIdAndWorkDate(Long userId, LocalDate workDate);

    long countByUserIdAndWorkDateBetween(Long userId, LocalDate start, LocalDate end);

    List<EmployeeWorkday> findByUserIdAndWorkDateBetweenOrderByWorkDateDesc(
            Long userId, LocalDate start, LocalDate end);
}
