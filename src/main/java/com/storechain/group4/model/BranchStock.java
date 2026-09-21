package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tồn kho của một sản phẩm (danh mục dùng chung toàn hãng) tại một chi nhánh.
 * Sản phẩm là danh mục chung, mỗi chi nhánh giữ số lượng riêng;
 * khi khách đặt hàng, đơn được phân phối về chi nhánh gần nhất còn hàng.
 */
@Entity
@Table(name = "branch_stocks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "shop_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_stock_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private int stock = 0;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
}
