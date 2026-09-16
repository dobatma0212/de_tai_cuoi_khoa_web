package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Chương trình khuyến mãi của chi nhánh:
 * - PERCENT: giảm % giá sản phẩm trong đơn
 * - SHIPPING: giảm phí vận chuyển (số tiền VND)
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
    @Column(name = "promo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Mã khuyến mãi để áp khi đặt hàng (duy nhất trong chi nhánh, có thể null) */
    @Column(name = "code", length = 30)
    private String code;

    /** PERCENT hoặc SHIPPING */
    @Column(name = "promo_type", nullable = false, length = 10)
    private String promoType;

    /** PERCENT: giá trị % (1..90); SHIPPING: số tiền giảm tối đa theo VND */
    @Column(name = "value", nullable = false, precision = 13, scale = 0)
    private BigDecimal value;

    /** Giá trị đơn tối thiểu để áp dụng (VND), 0 = không giới hạn */
    @Column(name = "min_order_amount", precision = 13, scale = 0)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.startDate == null) {
            this.startDate = LocalDateTime.now();
        }
    }
}
