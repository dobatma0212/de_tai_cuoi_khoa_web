package com.storechain.group4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private boolean isNew;
    private boolean isBestSeller;
    private boolean isFeatured;
    private boolean representative;

    /** Trạng thái: draft (Bản nháp), active (Đang mở bán), inactive (Ngừng kinh doanh) */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
