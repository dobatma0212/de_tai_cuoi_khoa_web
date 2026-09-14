package com.storechain.group4.repository;

import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}

