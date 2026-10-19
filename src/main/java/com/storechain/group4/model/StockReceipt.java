package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiếu nhập hàng cho một chi nhánh: manager lập nháp (DRAFT) rồi xác nhận (CONFIRMED)
 * để cộng tồn kho chi nhánh. Nhập hàng KHÔNG đụng vào products/products.stock —
 * chỉ ghi branch_stocks, kèm lịch sử biến động (stock_movements).
 */
@Entity
@Table(name = "stock_receipts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_receipt_code", columnNames = "receipt_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_receipt_id")
    private Long id;

    /** Mã phiếu duy nhất, dạng PN-yyyyMMdd-#### */
    @Column(name = "receipt_code", nullable = false, length = 30)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReceiptStatus status = ReceiptStatus.DRAFT;

    /** Nguồn nhập (nhà cung cấp, ghi chú nguồn hàng) */
    @Column(name = "source_note", length = 255)
    private String sourceNote;

    @Column(name = "note", length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /** Người xác nhận phiếu (null khi còn nháp) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private int totalQuantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<StockReceiptItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
