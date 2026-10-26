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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    /** Danh mục sản phẩm của chi nhánh / hãng */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ShopCategory category;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Mã gốc sản phẩm (Base SKU / Barcode, ví dụ: SM-OXF-01) */
    @Column(name = "base_sku", length = 100)
    private String baseSku;

    /** Thương hiệu sản phẩm (ví dụ: Male Fashion, Routine, Zara...) */
    @Column(name = "brand", length = 150)
    private String brand;

    /** Bộ sưu tập (ví dụ: Summer 2026, Thu Đông, Minimalist...) */
    @Column(name = "collection_name", length = 150)
    private String collectionName;

    /** Đường dẫn thân thiện cho SEO, duy nhất trên toàn hệ thống */
    @Column(name = "slug", length = 250, unique = true)
    private String slug;

    /** Thẻ tiêu đề SEO (Meta Title) */
    @Column(name = "meta_title", length = 250)
    private String metaTitle;

    /** Mô tả ngắn SEO (Meta Description) */
    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    /** Giá niêm yết bán theo VND */
    @Column(name = "price", nullable = false, precision = 13, scale = 0)
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Ảnh đại diện chính */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Bộ sưu tập ảnh sản phẩm (Gallery, định dạng JSON danh sách URL) */
    @Column(name = "gallery_images", columnDefinition = "TEXT")
    private String galleryImages;

    /** Bảng quy đổi kích cỡ (Size Chart) URL ảnh hoặc nội dung */
    @Column(name = "size_chart", columnDefinition = "TEXT")
    private String sizeChart;

    /** Số lượng tồn kho (tổng hợp từ branch_stocks) */
    @Column(name = "stock", nullable = false)
    @Builder.Default
    private int stock = 0;

    /** Các size còn hỗ trợ, phân tách bằng dấu phẩy (VD: S,M,L,XL) */
    @Column(name = "sizes", length = 200)
    private String sizes;

    /** Các màu của sản phẩm, phân tách bằng dấu phẩy (VD: Đen, Trắng, Xanh) */
    @Column(name = "color", length = 250)
    private String color;

    /** Danh sách các SKU con và ảnh riêng cho từng màu/biến thể (JSON) */
    @Column(name = "variants_json", columnDefinition = "LONGTEXT")
    private String variantsJson;

    /** Sản phẩm mới (New) */
    @Column(name = "is_new", nullable = false)
    @Builder.Default
    private boolean isNew = false;

    /** Sản phẩm bán chạy (Best Seller) */
    @Column(name = "is_best_seller", nullable = false)
    @Builder.Default
    private boolean isBestSeller = false;

    /** Gợi ý phối đồ / Tiêu biểu (Featured / Outfit Suggestion) */
    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean isFeatured = false;

    /** Sản phẩm đại diện cho danh mục (hiển thị ở trang chủ chi nhánh) */
    @Column(name = "is_representative", nullable = false)
    @Builder.Default
    private boolean representative = false;

    /** Trạng thái kinh doanh: draft (Bản nháp), active (Đang mở bán), inactive (Ngừng kinh doanh) */
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
