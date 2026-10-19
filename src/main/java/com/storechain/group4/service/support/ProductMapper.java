package com.storechain.group4.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ProductVariantDto;
import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.Product;
import com.storechain.group4.repository.BranchStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Mapper dùng chung cho manager/employee khi đọc sản phẩm:
 * trả đủ thuộc tính do admin cập nhật (SKU, thương hiệu, bộ sưu tập, SEO,
 * gallery, biến thể, nhãn, trạng thái) và tồn kho branchStock của CHỈ chi nhánh
 * của người gọi — không trả branchAllocations để không lộ tồn các chi nhánh khác.
 */
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final BranchStockRepository branchStockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param shopId chi nhánh của người gọi để tính branchStock; null thì lấy tổng aggregate
     */
    public ProductResponseDto toDto(Product p, Long shopId) {
        Integer branchStock = shopId == null
                ? p.getStock()
                : branchStockRepository.findByProductIdAndShopId(p.getId(), shopId)
                        .map(BranchStock::getStock)
                        .orElse(0);
        return ProductResponseDto.builder()
                .id(p.getId())
                .shopId(p.getShop() != null ? p.getShop().getId() : null)
                .shopName(p.getShop() != null ? p.getShop().getName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .name(p.getName())
                .baseSku(p.getBaseSku())
                .brand(p.getBrand())
                .collectionName(p.getCollectionName())
                .slug(p.getSlug())
                .metaTitle(p.getMetaTitle())
                .metaDescription(p.getMetaDescription())
                .price(p.getPrice())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .galleryImages(deserializeGallery(p.getGalleryImages()))
                .sizeChart(p.getSizeChart())
                .stock(p.getStock())
                .branchStock(branchStock)
                .sizes(p.getSizes())
                .sizesList(parseSizes(p.getSizes()))
                .color(p.getColor())
                .variants(deserializeVariants(p.getVariantsJson()))
                .isNew(p.isNew())
                .isBestSeller(p.isBestSeller())
                .isFeatured(p.isFeatured())
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private List<String> parseSizes(String sizes) {
        if (sizes == null || sizes.isBlank()) return List.of();
        return Arrays.stream(sizes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private List<String> deserializeGallery(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Arrays.stream(json.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
    }

    private List<ProductVariantDto> deserializeVariants(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ProductVariantDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
