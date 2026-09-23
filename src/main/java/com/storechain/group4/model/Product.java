package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "slug")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    /** Danh mục sản phẩm của chi nhánh (do manager quản lý) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ShopCategory category;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Đường dẫn thân thiện cho SEO, duy nhất trên toàn hệ thống (có thể null) */
    @Column(name = "slug", length = 250, unique = true)
    private String slug;

    /** Giá bán theo VND */
    @Column(name = "price", nullable = false, precision = 13, scale = 0)
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Số lượng tồn kho */
    @Column(name = "stock", nullable = false)
    @Builder.Default
    private int stock = 0;

    /** Các size còn hỗ trợ, phân tách bằng dấu phẩy (VD: S,M,L,XL) */
    @Column(name = "sizes", length = 200)
    private String sizes;

    /** Các màu của sản phẩm, phân tách bằng dấu phẩy (VD: Đen, Trắng) */
    @Column(name = "color", length = 250)
    private String color;

    /** Sản phẩm đại diện cho danh mục (hiển thị ở trang chủ chi nhánh) */
    @Column(name = "is_representative", nullable = false)
    @Builder.Default
    private boolean representative = false;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getProductId() {
        return this.id;
    }

    public void setProductId(Long productId) {
        this.id = productId;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(this.status);
    }
}
