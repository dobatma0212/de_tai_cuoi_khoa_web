package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sổ biến động kho của chi nhánh: mỗi lần tồn thay đổi (nhập, bán, hoàn trả)
 * ghi một dòng kèm tồn sau thay đổi (balance_after) để đối soát.
 * product_id nullable — khi admin xóa sản phẩm thì đặt null, giữ snapshot tên.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_movement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Snapshot tên sản phẩm lúc phát sinh */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "size", length = 20)
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MovementType type;

    /** Số lượng thay đổi (nhập/bán: dương; hủy nhập: âm) */
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    /** Tồn kho của sản phẩm tại chi nhánh sau biến động */
    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    /** Loại tài liệu tham chiếu: STOCK_RECEIPT / ORDER ... */
    @Column(name = "ref_type", length = 30)
    private String refType;

    /** Id tài liệu tham chiếu (phiếu nhập, đơn hàng...) */
    @Column(name = "ref_id")
    private Long refId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
