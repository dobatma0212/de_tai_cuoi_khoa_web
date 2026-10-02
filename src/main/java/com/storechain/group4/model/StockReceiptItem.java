package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Một dòng hàng trong phiếu nhập. Tên sản phẩm và SKU được snapshot lại
 * (product_id nullable) để khi admin xóa sản phẩm, lịch sử nhập vẫn đọc được.
 */
@Entity
@Table(name = "stock_receipt_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_receipt_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private StockReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Snapshot tên sản phẩm lúc nhập */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /** Snapshot SKU gốc của sản phẩm lúc nhập */
    @Column(name = "base_sku", length = 60)
    private String baseSku;

    /** Size nhập (null với sản phẩm không phân size) */
    @Column(name = "size", length = 20)
    private String size;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
