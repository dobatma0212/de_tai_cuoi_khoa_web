package com.storechain.group4.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Chương trình khuyến mãi.
 * - Khuyến mãi do admin tạo: shop = null, áp dụng cho toàn server.
 * - Khuyến mãi do manager tạo: shop = chi nhánh sở hữu, chỉ áp dụng trong chi nhánh đó
 *   (promoType: PERCENT = giảm % giá đơn; SHIPPING = giảm phí vận chuyển).
 */
@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_SHIPPING = "SHIPPING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long id;

    /** Chi nhánh sở hữu khuyến mãi; null = khuyến mãi toàn server (do admin tạo) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 30)
    private PromotionType promotionType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Mã khuyến mãi để áp khi đặt hàng (duy nhất trong phạm vi chi nhánh với manager, toàn server với admin; có thể null) */
    @Column(name = "code", length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    /** PERCENT hoặc SHIPPING: chỉ dùng cho khuyến mãi của chi nhánh; null với khuyến mãi toàn server */
    @Column(name = "promo_type", length = 10)
    private String promoType;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "promotion_products",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Product> products = new HashSet<>();

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
        if (this.startDate == null) {
            this.startDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getPromotionId() {
        return this.id;
    }

    public void setPromotionId(Long promotionId) {
        this.id = promotionId;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(this.status);
    }

    public void setActive(boolean active) {
        this.status = active ? "active" : "inactive";
    }

    public boolean isExpired() {
        return this.endDate != null && LocalDateTime.now().isAfter(this.endDate);
    }

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterOrAtStart = this.startDate == null || !now.isBefore(this.startDate);
        boolean beforeOrAtEnd = this.endDate == null || !now.isAfter(this.endDate);
        return isActive() && afterOrAtStart && beforeOrAtEnd;
    }
}
