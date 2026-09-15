package com.storechain.group4.repository;

import com.storechain.group4.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByParentId(Long parentId);

    long countByParentId(Long parentId);

    List<Category> findByParentIsNullOrderBySortOrderAsc();

    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);

    List<Category> findByStatusOrderBySortOrderAsc(String status);

    List<Category> findByParentIsNullAndStatusOrderBySortOrderAsc(String status);

    @Query("SELECT c FROM Category c WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR LOWER(c.status) = LOWER(:status)) " +
            "AND (:parentId IS NULL OR c.parent.id = :parentId) " +
            "AND (:onlyRoots IS NULL OR :onlyRoots = false OR c.parent IS NULL)")
    Page<Category> searchCategories(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("parentId") Long parentId,
            @Param("onlyRoots") Boolean onlyRoots,
            Pageable pageable
    );

    long countByStatus(String status);

    long countByParentIsNull();
}
