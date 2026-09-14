package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", length = 50, unique = true, nullable = false)
    private RoleType name;

    @Column(name = "description", length = 255)
    private String description;

    public Role(RoleType name) {
        this.name = name;
    }

    public Role(RoleType name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getRoleId() {
        return this.id;
    }

    public void setRoleId(Long roleId) {
        this.id = roleId;
    }
}
