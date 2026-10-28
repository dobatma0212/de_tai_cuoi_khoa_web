package com.storechain.group4.service;

import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.model.DiscountType;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Promotion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PromotionCalculator {

    /**
     * Calculate discount amount for a voucher applied to an order.
     */
    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal orderTotal) {
        if (promotion == null || orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderTotal.multiply(promotion.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (promotion.getMaxDiscountAmount() != null) {
                discount = discount.min(promotion.getMaxDiscountAmount());
            }
        } else if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = promotion.getDiscountValue();
        }

        return discount.min(orderTotal);
    }

    /**
     * Calculate product price after flash sale.
     */
    public BigDecimal calculateFlashSalePrice(Promotion flashSale, BigDecimal originalPrice) {
        if (flashSale == null || originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return originalPrice != null ? originalPrice : BigDecimal.ZERO;
        }

        BigDecimal priceAfterDiscount = originalPrice;
        if (flashSale.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal discount = originalPrice.multiply(flashSale.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (flashSale.getMaxDiscountAmount() != null
                    && flashSale.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(flashSale.getMaxDiscountAmount());
            }
            priceAfterDiscount = originalPrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        } else if (flashSale.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            priceAfterDiscount = originalPrice.subtract(flashSale.getDiscountValue());
        }

        return priceAfterDiscount.max(BigDecimal.ZERO);
    }

    /**
     * Kiểm tra một chương trình Flash Sale có áp dụng cho sản phẩm hay không.
     * Nếu danh sách products của Flash Sale rỗng/null thì hiểu là áp dụng toàn cửa hàng.
     */
    public boolean isProductInFlashSale(Promotion flashSale, Long productId) {
        if (flashSale == null || productId == null) {
            return false;
        }
        if (flashSale.getProducts() == null || flashSale.getProducts().isEmpty()) {
            return true;
        }
        return flashSale.getProducts().stream()
                .anyMatch(prod -> prod != null && productId.equals(prod.getId()));
    }

    /**
     * Tìm chương trình Flash Sale đang hoạt động mang lại mức giá thấp nhất cho sản phẩm.
     */
    public Promotion findBestFlashSale(List<Promotion> activeFlashSales, Long productId, BigDecimal originalPrice) {
        if (activeFlashSales == null || activeFlashSales.isEmpty()
                || productId == null || originalPrice == null
                || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        Promotion best = null;
        BigDecimal bestPrice = originalPrice;
        for (Promotion fs : activeFlashSales) {
            if (!isProductInFlashSale(fs, productId)) {
                continue;
            }
            BigDecimal candidate = calculateFlashSalePrice(fs, originalPrice);
            if (candidate.compareTo(bestPrice) < 0) {
                bestPrice = candidate;
                best = fs;
            }
        }
        return best;
    }

    /**
     * Tính giá hiệu lực thực tế của sản phẩm (đã áp dụng Flash Sale nếu có).
     */
    public BigDecimal resolveEffectivePrice(List<Promotion> activeFlashSales, Product product) {
        if (product == null || product.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        Promotion best = findBestFlashSale(activeFlashSales, product.getId(), product.getPrice());
        return best != null ? calculateFlashSalePrice(best, product.getPrice()) : product.getPrice();
    }

    /**
     * Gắn thông tin giá gốc, giá sau giảm và trạng thái Flash Sale vào ProductResponseDto.
     */
    public void applyFlashSaleToDto(ProductResponseDto dto, BigDecimal originalPrice, List<Promotion> activeFlashSales) {
        if (dto == null) {
            return;
        }
        dto.setOriginalPrice(originalPrice);
        Promotion best = findBestFlashSale(activeFlashSales, dto.getId(), originalPrice);
        if (best != null && originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal salePrice = calculateFlashSalePrice(best, originalPrice);
            dto.setPrice(salePrice);
            dto.setSalePrice(salePrice);
            dto.setOnFlashSale(true);
            dto.setFlashSaleName(best.getName());
            int pct = originalPrice.subtract(salePrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
                    .intValue();
            dto.setDiscountPercent(Math.max(1, pct));
        } else {
            dto.setPrice(originalPrice);
            dto.setSalePrice(null);
            dto.setOnFlashSale(false);
            dto.setFlashSaleName(null);
            dto.setDiscountPercent(null);
        }
    }

    /**
     * Validate if a voucher can be applied. Returns error message or null if valid.
     */
    public String validateVoucher(Promotion promotion, BigDecimal orderTotal, long userUsageCount) {
        if (promotion == null) {
            return "Khuyến mãi không tồn tại";
        }
        if (!"active".equalsIgnoreCase(promotion.getStatus())) {
            return "Khuyến mãi không hoạt động";
        }
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            return "Khuyến mãi chưa bắt đầu";
        }
        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            return "Khuyến mãi đã hết hạn";
        }
        if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            return "Khuyến mãi đã hết lượt sử dụng";
        }
        if (promotion.getUsageLimitPerUser() != null && userUsageCount >= promotion.getUsageLimitPerUser()) {
            return "Bạn đã hết lượt sử dụng khuyến mãi này";
        }
        if (promotion.getMinOrderValue() != null && orderTotal.compareTo(promotion.getMinOrderValue()) < 0) {
            return "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng khuyến mãi";
        }
        if (promotion.getDiscountValue() == null || promotion.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            return "Giá trị khuyến mãi không hợp lệ";
        }
        return null;
    }
}
