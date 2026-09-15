package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transporters",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transporter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transporter_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.baseFee == null) {
            this.baseFee = BigDecimal.ZERO;
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "active";
        }
    }

    public Long getTransporterId() {
        return this.id;
    }

    public void setTransporterId(Long transporterId) {
        this.id = transporterId;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(this.status);
    }
}
