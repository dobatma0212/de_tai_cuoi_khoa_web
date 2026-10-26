package com.storechain.group4.service;

import com.storechain.group4.dto.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {
    // Admin CRUD
    Page<PromotionResponseDto> getPromotions(String keyword, String status, String promotionType, int page, int size, String sortBy, String sortDir);
    PromotionResponseDto getPromotionById(Long id);
    PromotionResponseDto createPromotion(PromotionRequestDto request);
    PromotionResponseDto updatePromotion(Long id, PromotionRequestDto request);
    PromotionResponseDto updateStatus(Long id, String status);
    void deletePromotion(Long id);
    PromotionStatsDto getStats();
    List<ProductOptionDto> getProductOptions();
    
    // Public API
    List<PromotionResponseDto> getActiveFlashSales();
    /** Danh sách voucher (toàn server) đang có hiệu lực để user chọn khi đặt hàng */
    List<PromotionResponseDto> getAvailableVouchers();
    ApplyVoucherResponseDto checkVoucher(String code, BigDecimal orderTotal, Long userId);
    ApplyVoucherResponseDto applyVoucher(String code, BigDecimal orderTotal, Long userId);
}
