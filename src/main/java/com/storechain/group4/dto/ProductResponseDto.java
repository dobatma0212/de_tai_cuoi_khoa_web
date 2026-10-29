package com.storechain.group4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {

    private Long id;
    private String name;
    private String baseSku;
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private String collectionName;
    private String slug;
    private String metaTitle;
    private String metaDescription;
    private BigDecimal price;
    /** Giá niêm yết gốc của sản phẩm (trước khi áp dụng Flash Sale) */
    private BigDecimal originalPrice;
    /** Giá sau khi giảm Flash Sale (null nếu sản phẩm không nằm trong Flash Sale đang chạy) */
    private BigDecimal salePrice;
    /** Cờ đánh dấu sản phẩm đang được giảm giá Flash Sale */
    private boolean onFlashSale;
    /** Tên chương trình Flash Sale đang áp dụng */
    private String flashSaleName;
    /** Phần trăm giảm giá tương đương (làm tròn) để hiển thị badge -XX% */
    private Integer discountPercent;
    private String description;
    private String imageUrl;
    private List<String> galleryImages;
    private String sizeChart;

    /** Tổng tồn kho aggregate từ bảng kho của tất cả chi nhánh */
    private Integer stock;
    private Integer branchStock;

    private String sizes;
    private List<String> sizesList;

    /** Tồn kho của chi nhánh đang xem theo từng size (null nếu kho chưa phân bổ size) */
    private Map<String, Integer> branchSizeStocks;

    /** Tồn kho theo size TỔNG HỢP toàn hệ thống (dùng ở storefront, khi không xem theo chi nhánh) */
    private Map<String, Integer> totalSizeStocks;
    private String color;

    /** Danh sách các biến thể con SKU tự sinh (kèm ảnh riêng) */
    private List<ProductVariantDto> variants;

    /** Bảng phân bổ tồn kho chi nhánh theo từng size */
    private List<BranchAllocationDto> branchAllocations;

    private Double avgRating;
    private Long reviewCount;

    /** Số lượng đã bán (không tính đơn hủy/trả hàng) */
    private Long soldCount;

    /** Số lượt yêu thích */
    private Long favoriteCount;

    /** Nhãn website */
    @JsonProperty("isNew")
    private boolean isNew;
    @JsonProperty("isBestSeller")
    private boolean isBestSeller;
    @JsonProperty("isFeatured")
    private boolean isFeatured;
    private boolean representative;

    @JsonProperty("isNew")
    public boolean isNew() {
        return isNew;
    }

    @JsonProperty("isBestSeller")
    public boolean isBestSeller() {
        return isBestSeller;
    }

    @JsonProperty("isFeatured")
    public boolean isFeatured() {
        return isFeatured;
    }

    /** Trạng thái: draft (Bản nháp), active (Đang mở bán), inactive (Ngừng kinh doanh) */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
