package com.storechain.group4.repository;

import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:roleType IS NULL OR u.role.name = :roleType) " +
            "AND (:status IS NULL OR :status = '' OR LOWER(u.status) = LOWER(:status))")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("roleType") RoleType roleType,
            @Param("status") String status,
            Pageable pageable
    );

    long countByStatus(String status);

    long countByRole(Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleType")
    long countByRoleType(@Param("roleType") RoleType roleType);

    @Query("SELECT u FROM User u WHERE u.role.name = :roleType ORDER BY u.fullName ASC")
    java.util.List<User> findByRoleType(@Param("roleType") RoleType roleType);
}
