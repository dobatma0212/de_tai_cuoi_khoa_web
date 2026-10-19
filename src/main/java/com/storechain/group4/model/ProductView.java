package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lịch sử sản phẩm đã xem của người dùng.
 */
@Entity
@Table(name = "product_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Số lần xem */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 1;

    @Column(name = "first_viewed_at", nullable = false, updatable = false)
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.firstViewedAt == null) {
            this.firstViewedAt = now;
        }
        this.lastViewedAt = now;
        if (this.viewCount < 1) {
            this.viewCount = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastViewedAt = LocalDateTime.now();
    }
}
