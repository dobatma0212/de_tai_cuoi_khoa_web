package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    // Trạng thái đơn hàng của chi nhánh
    public static final String STATUS_NEW = "NEW";                     // Đơn hàng mới
    public static final String STATUS_CONFIRMED = "CONFIRMED";         // Đã xác nhận
    public static final String STATUS_PICKED_UP = "PICKED_UP";         // Đã lấy hàng
    public static final String STATUS_SHIPPING = "SHIPPING";           // Đang giao
    public static final String STATUS_DELIVERED = "DELIVERED";         // Đã giao
    public static final String STATUS_RECEIVED = "RECEIVED";           // Khách đã nhận hàng
    public static final String STATUS_CANCELLED = "CANCELLED";         // Hủy
    public static final String STATUS_RETURNED = "RETURNED";           // Trả hàng - hoàn tiền
    public static final String STATUS_RETURN_REQUESTED = "RETURN_REQUESTED"; // Khách yêu cầu trả hàng

    // Loại đơn hàng: online (đặt qua web) hoặc offline (bán trực tiếp tại quầy)
    public static final String TYPE_ONLINE = "onl";
    public static final String TYPE_OFFLINE = "off";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_code", nullable = false, length = 30, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_NEW;

    @Column(name = "total_amount", nullable = false, precision = 13, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 13, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 13, scale = 0)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 13, scale = 0)
    private BigDecimal grandTotal;

    @Column(name = "order_type", length = 10)
    @Builder.Default
    private String orderType = TYPE_ONLINE;

    /** Tên chương trình khuyến mãi áp dụng (nếu có) */
    @Column(name = "promo_name", length = 150)
    private String promoName;

    /** Nhà vận chuyển */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id")
    private Transporter transporter;

    /** Nhân viên phụ trách đơn (bán trực tiếp tại chi nhánh / xử lý đơn), dùng tính hoa hồng */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    /** Địa chỉ nhận hàng snapshot */
    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 15)
    private String receiverPhone;

    @Column(name = "receiver_address", length = 500)
    private String receiverAddress;

    @Column(name = "note", length = 500)
    private String note;

    /** Lý do hủy đơn (do chi nhánh hoặc khách nhập) */
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.status == null || this.status.isBlank()) {
            this.status = STATUS_NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
