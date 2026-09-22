package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tồn kho của một sản phẩm tại một chi nhánh.
 * Đơn hàng được phân phối về chi nhánh còn đủ hàng gần địa chỉ nhận nhất.
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
}
