package com.storechain.group4.repository;

import com.storechain.group4.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);

    List<SearchHistory> findByUserIdAndShopIdOrderBySearchedAtDesc(Long userId, Long shopId, Pageable pageable);

    Optional<SearchHistory> findByUserIdAndKeywordIgnoreCase(Long userId, String keyword);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndShopId(Long userId, Long shopId);
}
