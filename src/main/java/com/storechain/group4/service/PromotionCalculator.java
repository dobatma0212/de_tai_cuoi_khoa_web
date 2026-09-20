package com.storechain.group4.service;

import com.storechain.group4.model.DiscountType;
import com.storechain.group4.model.Promotion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                    flashSale.getDiscountValue().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
            priceAfterDiscount = originalPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        } else if (flashSale.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            priceAfterDiscount = originalPrice.subtract(flashSale.getDiscountValue());
        }

        return priceAfterDiscount.max(BigDecimal.ZERO);
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
