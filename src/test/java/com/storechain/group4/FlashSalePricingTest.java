package com.storechain.group4;

import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.PromotionRequestDto;
import com.storechain.group4.model.DiscountType;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Promotion;
import com.storechain.group4.model.PromotionType;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.PromotionUsageRepository;
import com.storechain.group4.service.PromotionCalculator;
import com.storechain.group4.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlashSalePricingTest {

    private final PromotionCalculator calculator = new PromotionCalculator();

    @Test
    void applyFlashSaleToDto_specificAndAllProductsScope() {
        Product p1 = Product.builder().id(1L).name("Áo Polo").price(new BigDecimal("300000")).build();
        Product p2 = Product.builder().id(2L).name("Quần Jeans").price(new BigDecimal("500000")).build();

        Promotion specificSale = Promotion.builder()
                .id(10L)
                .name("Flash Sale Áo Polo 20%")
                .promotionType(PromotionType.FLASH_SALE)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20"))
                .status("active")
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusHours(5))
                .products(new HashSet<>(Set.of(p1)))
                .build();

        ProductResponseDto dto1 = ProductResponseDto.builder().id(1L).price(p1.getPrice()).build();
        calculator.applyFlashSaleToDto(dto1, p1.getPrice(), List.of(specificSale));
        assertTrue(dto1.isOnFlashSale());
        assertEquals(0, new BigDecimal("240000").compareTo(dto1.getPrice()));
        assertEquals(0, new BigDecimal("300000").compareTo(dto1.getOriginalPrice()));
        assertEquals(20, dto1.getDiscountPercent());

        ProductResponseDto dto2 = ProductResponseDto.builder().id(2L).price(p2.getPrice()).build();
        calculator.applyFlashSaleToDto(dto2, p2.getPrice(), List.of(specificSale));
        assertFalse(dto2.isOnFlashSale());
        assertEquals(0, new BigDecimal("500000").compareTo(dto2.getPrice()));

        // Store-wide Flash Sale with maxDiscountAmount cap
        Promotion storeWideSale = Promotion.builder()
                .id(11L)
                .name("Flash Sale Toàn Shop 50% (Tối đa 100k)")
                .promotionType(PromotionType.FLASH_SALE)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("50"))
                .maxDiscountAmount(new BigDecimal("100000"))
                .status("active")
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusHours(5))
                .products(new HashSet<>())
                .build();

        calculator.applyFlashSaleToDto(dto2, p2.getPrice(), List.of(specificSale, storeWideSale));
        assertTrue(dto2.isOnFlashSale());
        assertEquals(0, new BigDecimal("400000").compareTo(dto2.getPrice()));
        assertEquals(0, new BigDecimal("500000").compareTo(dto2.getOriginalPrice()));
        assertEquals(20, dto2.getDiscountPercent());
    }

    @Test
    void updatePromotion_flashSaleSwitchToAllProductsAssignsAllProducts() {
        PromotionRepository promoRepo = mock(PromotionRepository.class);
        PromotionUsageRepository usageRepo = mock(PromotionUsageRepository.class);
        ProductRepository prodRepo = mock(ProductRepository.class);
        PromotionServiceImpl service = new PromotionServiceImpl(promoRepo, usageRepo, prodRepo, calculator);

        Product p1 = Product.builder().id(1L).name("SP 1").price(new BigDecimal("100000")).build();
        Product p2 = Product.builder().id(2L).name("SP 2").price(new BigDecimal("200000")).build();

        Promotion existing = Promotion.builder()
                .id(5L)
                .name("Sale cũ")
                .promotionType(PromotionType.FLASH_SALE)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10"))
                .status("active")
                .products(new HashSet<>(Set.of(p1)))
                .build();

        when(promoRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(prodRepo.findAll()).thenReturn(List.of(p1, p2));
        when(promoRepo.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        PromotionRequestDto req = PromotionRequestDto.builder()
                .name("Sale toàn cửa hàng")
                .promotionType("FLASH_SALE")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("15"))
                .startDate("2026-01-01 00:00:00")
                .endDate("2026-12-31 23:59:59")
                .status("active")
                .productIds(null)
                .build();

        var res = service.updatePromotion(5L, req);
        assertEquals(2, res.getProductIds().size());
        assertTrue(res.getProductIds().containsAll(List.of(1L, 2L)));
    }
}
