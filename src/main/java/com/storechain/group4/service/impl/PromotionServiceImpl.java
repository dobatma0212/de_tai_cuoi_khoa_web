package com.storechain.group4.service.impl;

import com.storechain.group4.dto.*;
import com.storechain.group4.model.*;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.PromotionUsageRepository;
import com.storechain.group4.service.PromotionCalculator;
import com.storechain.group4.service.PromotionService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ProductRepository productRepository;
    private final PromotionCalculator promotionCalculator;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "code", "promotionType", "discountType", "discountValue", "status", "startDate", "endDate", "createdAt"
    );

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponseDto> getPromotions(String keyword, String status, String promotionType, int page, int size, String sortBy, String sortDir) {
        if (sortBy == null || sortBy.isBlank() || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "id";
        }
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PromotionType typeEnum = null;
        if (promotionType != null && !promotionType.isBlank()) {
            try {
                typeEnum = PromotionType.valueOf(promotionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid type
            }
        }

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty();
        boolean hasType = typeEnum != null;

        // Nếu không có bộ lọc nào (trường hợp load mặc định của trang), dùng findAll để tránh lỗi null param của JPQL
        if (!hasKeyword && !hasStatus && !hasType) {
            return promotionRepository.findAll(pageable).map(this::toDto);
        }

        // Nếu chỉ lọc theo loại chương trình
        if (!hasKeyword && !hasStatus && hasType) {
            return promotionRepository.findByPromotionType(typeEnum, pageable).map(this::toDto);
        }

        // Nếu chỉ lọc theo trạng thái
        if (!hasKeyword && hasStatus && !hasType) {
            return promotionRepository.findByStatus(status.trim(), pageable).map(this::toDto);
        }

        // Nếu lọc theo cả loại và trạng thái (không có từ khóa)
        if (!hasKeyword && hasStatus && hasType) {
            return promotionRepository.findByPromotionTypeAndStatus(typeEnum, status.trim(), pageable).map(this::toDto);
        }

        // Trường hợp có từ khóa hoặc kết hợp phức tạp
        String searchKeyword = hasKeyword ? keyword.trim() : null;
        String searchStatus = hasStatus ? status.trim() : null;
        return promotionRepository.searchPromotions(searchKeyword, searchStatus, typeEnum, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponseDto getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi với ID: " + id));
        return toDto(promotion);
    }

    @Override
    @Transactional
    public PromotionResponseDto createPromotion(PromotionRequestDto request) {
        PromotionType pType = PromotionType.valueOf(request.getPromotionType().toUpperCase());
        DiscountType dType = DiscountType.valueOf(request.getDiscountType().toUpperCase());

        String code = null;
        if (pType == PromotionType.VOUCHER) {
            if (request.getCode() == null || request.getCode().isBlank()) {
                throw new IllegalArgumentException("Mã voucher không được để trống");
            }
            code = request.getCode().trim().toUpperCase();
            if (promotionRepository.existsByCodeIgnoreCaseAndShopIsNull(code)) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại");
            }
        }

        LocalDateTime start = parseDateTime(request.getStartDate());
        LocalDateTime end = parseDateTime(request.getEndDate());
        if (start == null || end == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc không hợp lệ");
        }
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị khuyến mãi phải lớn hơn 0");
        }
        if (dType == DiscountType.PERCENTAGE && request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Giá trị phần trăm khuyến mãi không được vượt quá 100");
        }

        String promoStatus = (request.getStatus() != null && !request.getStatus().isBlank())
                ? request.getStatus().toLowerCase()
                : "active";

        Promotion promotion = Promotion.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .code(code)
                .promotionType(pType)
                .discountType(dType)
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .startDate(start)
                .endDate(end)
                .usageLimit(request.getUsageLimit())
                .usageLimitPerUser(request.getUsageLimitPerUser())
                .minOrderValue(request.getMinOrderValue())
                .status(promoStatus)
                .usedCount(0)
                .build();

        if (pType == PromotionType.FLASH_SALE) {
            if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
                List<Product> products = productRepository.findAllById(request.getProductIds());
                promotion.setProducts(new HashSet<>(products));
            } else {
                List<Product> allProducts = productRepository.findAll();
                promotion.setProducts(new HashSet<>(allProducts));
            }
        }

        Promotion saved = promotionRepository.save(promotion);
        log.info("Đã tạo khuyến mãi mới: ID={}, name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public PromotionResponseDto updatePromotion(Long id, PromotionRequestDto request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi với ID: " + id));

        PromotionType pType = PromotionType.valueOf(request.getPromotionType().toUpperCase());
        DiscountType dType = DiscountType.valueOf(request.getDiscountType().toUpperCase());

        String code = null;
        if (pType == PromotionType.VOUCHER) {
            if (request.getCode() == null || request.getCode().isBlank()) {
                throw new IllegalArgumentException("Mã voucher không được để trống");
            }
            code = request.getCode().trim().toUpperCase();
            if (promotionRepository.existsByCodeIgnoreCaseAndShopIsNullAndIdNot(code, id)) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại");
            }
        }

        LocalDateTime start = parseDateTime(request.getStartDate());
        LocalDateTime end = parseDateTime(request.getEndDate());
        if (start == null || end == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc không hợp lệ");
        }
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị khuyến mãi phải lớn hơn 0");
        }
        if (dType == DiscountType.PERCENTAGE && request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Giá trị phần trăm khuyến mãi không được vượt quá 100");
        }

        promotion.setName(request.getName().trim());
        promotion.setDescription(request.getDescription());
        promotion.setCode(code);
        promotion.setPromotionType(pType);
        promotion.setDiscountType(dType);
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setStartDate(start);
        promotion.setEndDate(end);
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setUsageLimitPerUser(request.getUsageLimitPerUser());
        promotion.setMinOrderValue(request.getMinOrderValue());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            promotion.setStatus(request.getStatus().toLowerCase());
        }

        if (pType == PromotionType.FLASH_SALE) {
            if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
                List<Product> products = productRepository.findAllById(request.getProductIds());
                promotion.setProducts(new HashSet<>(products));
            } else {
                List<Product> allProducts = productRepository.findAll();
                promotion.setProducts(new HashSet<>(allProducts));
            }
        } else if (promotion.getProducts() != null) {
            promotion.getProducts().clear();
        }
        if (false) {
            List<Product> products = productRepository.findAllById(request.getProductIds());
            promotion.setProducts(new HashSet<>(products));
        }

        Promotion updated = promotionRepository.save(promotion);
        log.info("Đã cập nhật khuyến mãi: ID={}, name={}", updated.getId(), updated.getName());
        return toDto(updated);
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String clean = dateStr.trim().replace("T", " ");
        if (clean.length() == 16) {
            clean += ":00";
        } else if (clean.length() > 19) {
            clean = clean.substring(0, 19);
        }
        return LocalDateTime.parse(clean, DATE_FORMATTER);
    }

    @Override
    @Transactional
    public PromotionResponseDto updateStatus(Long id, String status) {
        if (!"active".equalsIgnoreCase(status) && !"inactive".equalsIgnoreCase(status) && !"expired".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'active', 'inactive' hoặc 'expired'");
        }
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi với ID: " + id));

        promotion.setStatus(status.toLowerCase());
        Promotion saved = promotionRepository.save(promotion);
        log.info("Đã cập nhật trạng thái khuyến mãi ID {} thành {}", id, status);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi với ID: " + id));

        promotionUsageRepository.deleteByPromotionId(id);
        promotionRepository.delete(promotion);
        log.info("Đã xóa khuyến mãi ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionStatsDto getStats() {
        long total = promotionRepository.count();
        long active = promotionRepository.countByStatus("active");
        long expired = promotionRepository.countByStatus("expired");
        long vouchers = promotionRepository.countByPromotionType(PromotionType.VOUCHER);
        long activeVouchers = promotionRepository.countByPromotionTypeAndStatus(PromotionType.VOUCHER, "active");
        long flashSales = promotionRepository.countByPromotionType(PromotionType.FLASH_SALE);
        long activeFlashSales = promotionRepository.countByPromotionTypeAndStatus(PromotionType.FLASH_SALE, "active");

        return PromotionStatsDto.builder()
                .totalPromotions(total)
                .activePromotions(active)
                .expiredPromotions(expired)
                .totalVouchers(vouchers)
                .activeVouchers(activeVouchers)
                .totalFlashSales(flashSales)
                .activeFlashSales(activeFlashSales)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponseDto> getActiveFlashSales() {
        return promotionRepository.findActiveFlashSales(LocalDateTime.now())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponseDto> getAvailableVouchers() {
        LocalDateTime now = LocalDateTime.now();
        // Gộp cả voucher toàn server (admin tạo) và voucher của từng chi nhánh (manager tạo)
        List<PromotionResponseDto> result = promotionRepository.findAvailableVouchers(now).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        promotionRepository.findAvailableShopVouchers(now).stream()
                .map(this::toDto)
                .forEach(result::add);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyVoucherResponseDto checkVoucher(String code, BigDecimal orderTotal, Long userId) {
        // Ưu tiên voucher toàn server; không thấy thì tìm trong voucher của các chi nhánh
        Promotion promotion = promotionRepository.findByCodeIgnoreCaseAndShopIsNull(code)
                .or(() -> promotionRepository.findActiveShopVouchersByCode(code).stream().findFirst())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại"));

        long userUsageCount = promotionUsageRepository.countByPromotionIdAndUserId(promotion.getId(), userId);
        String error = promotionCalculator.validateVoucher(promotion, orderTotal, userUsageCount);

        if (error != null) {
            return ApplyVoucherResponseDto.builder()
                    .valid(false)
                    .promotionId(promotion.getId())
                    .promotionName(promotion.getName())
                    .code(promotion.getCode())
                    .message(error)
                    .discountAmount(BigDecimal.ZERO)
                    .finalTotal(orderTotal)
                    .build();
        }

        boolean isShopShipping = promotion.getShop() != null
                && Promotion.TYPE_SHIPPING.equals(promotion.getPromoType());
        BigDecimal discountAmount = promotionCalculator.calculateDiscount(promotion, orderTotal);
        BigDecimal finalTotal = orderTotal.subtract(discountAmount).max(BigDecimal.ZERO);
        String message = isShopShipping
                ? "Mã giảm phí vận chuyển " + promotion.getDiscountValue().longValue()
                    + "₫ — áp dụng khi đơn được chi nhánh \"" + promotion.getShop().getName() + "\" xử lý"
                : promotion.getShop() != null
                    ? "Mã voucher hợp lệ — áp dụng tại chi nhánh \"" + promotion.getShop().getName() + "\""
                    : "Mã voucher hợp lệ";
        return ApplyVoucherResponseDto.builder()
                .valid(true)
                .promotionId(promotion.getId())
                .promotionName(promotion.getName())
                .code(promotion.getCode())
                .promoType(promotion.getPromoType())
                .discountType(promotion.getDiscountType() != null ? promotion.getDiscountType().name() : null)
                .discountValue(promotion.getDiscountValue())
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public ApplyVoucherResponseDto applyVoucher(String code, BigDecimal orderTotal, Long userId) {
        int maxRetries = 3;
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                Promotion promotion = promotionRepository.findByCodeIgnoreCaseAndShopIsNull(code)
                        .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại"));

                promotion = promotionRepository.findByIdForUpdate(promotion.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Không thể khóa dòng dữ liệu khuyến mãi"));

                long userUsageCount = promotionUsageRepository.countByPromotionIdAndUserId(promotion.getId(), userId);
                String error = promotionCalculator.validateVoucher(promotion, orderTotal, userUsageCount);
                if (error != null) {
                    return ApplyVoucherResponseDto.builder()
                            .valid(false)
                            .promotionId(promotion.getId())
                            .promotionName(promotion.getName())
                            .code(promotion.getCode())
                            .message(error)
                            .discountAmount(BigDecimal.ZERO)
                            .finalTotal(orderTotal)
                            .build();
                }

                int updated = promotionRepository.incrementUsedCount(promotion.getId());
                if (updated == 0) {
                    throw new IllegalArgumentException("Voucher đã hết lượt sử dụng");
                }

                BigDecimal discountAmount = promotionCalculator.calculateDiscount(promotion, orderTotal);
                BigDecimal finalTotal = orderTotal.subtract(discountAmount).max(BigDecimal.ZERO);

                PromotionUsage usage = PromotionUsage.builder()
                        .promotion(promotion)
                        .userId(userId)
                        .discountAmount(discountAmount)
                        .orderTotal(orderTotal)
                        .build();
                promotionUsageRepository.save(usage);

                log.info("Đã áp dụng voucher {} cho userId {}", code, userId);

                return ApplyVoucherResponseDto.builder()
                        .valid(true)
                        .promotionId(promotion.getId())
                        .promotionName(promotion.getName())
                        .code(promotion.getCode())
                        .discountType(promotion.getDiscountType() != null ? promotion.getDiscountType().name() : null)
                        .discountValue(promotion.getDiscountValue())
                        .discountAmount(discountAmount)
                        .finalTotal(finalTotal)
                        .message("Áp dụng voucher thành công")
                        .build();

            } catch (OptimisticLockException e) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw new IllegalStateException("Hệ thống đang bận, vui lòng thử lại sau");
                }
                log.warn("Lỗi OptimisticLockException khi áp dụng voucher {}, thử lại lần {}", code, attempts);
            }
        }
        
        throw new IllegalStateException("Hệ thống đang bận, vui lòng thử lại sau");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductOptionDto> getProductOptions() {
        return productRepository.findAll().stream()
                .map(p -> {
                    String catName = null;
                    try {
                        if (p.getCategory() != null) {
                            catName = p.getCategory().getName();
                        }
                    } catch (Exception ignored) {}
                    return ProductOptionDto.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .price(p.getPrice())
                            .imageUrl(p.getImageUrl())
                            .categoryName(catName)
                            .stock(p.getStock())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private PromotionResponseDto toDto(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        boolean expired = promotion.getEndDate() != null && promotion.getEndDate().isBefore(now);
        boolean currentlyActive = "active".equalsIgnoreCase(promotion.getStatus()) && !expired
                && (promotion.getStartDate() == null || !promotion.getStartDate().isAfter(now));

        Integer remainingUsage = null;
        if (promotion.getUsageLimit() != null) {
            remainingUsage = promotion.getUsageLimit() - promotion.getUsedCount();
            if (remainingUsage < 0) remainingUsage = 0;
        }

        List<Long> productIds = new java.util.ArrayList<>();
        List<ProductOptionDto> productDtos = new java.util.ArrayList<>();
        try {
            if (promotion.getProducts() != null) {
                for (Product p : promotion.getProducts()) {
                    if (p != null) {
                        productIds.add(p.getId());
                        String catName = null;
                        try {
                            if (p.getCategory() != null) {
                                catName = p.getCategory().getName();
                            }
                        } catch (Exception ignored) {}

                        productDtos.add(ProductOptionDto.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .price(p.getPrice())
                                .imageUrl(p.getImageUrl())
                                .categoryName(catName)
                                .stock(p.getStock())
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể load products cho promotion id={}: {}", promotion.getId(), e.getMessage());
        }

        Long shopId = null;
        String shopName = null;
        try {
            if (promotion.getShop() != null) {
                shopId = promotion.getShop().getId();
                shopName = promotion.getShop().getName();
            }
        } catch (Exception e) {
            log.warn("Không thể load shop cho promotion id={}: {}", promotion.getId(), e.getMessage());
        }

        return PromotionResponseDto.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .code(promotion.getCode())
                .promotionType(promotion.getPromotionType() != null ? promotion.getPromotionType().name() : null)
                .promoType(promotion.getPromoType())
                .discountType(promotion.getDiscountType() != null ? promotion.getDiscountType().name() : null)
                .discountValue(promotion.getDiscountValue())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .usageLimit(promotion.getUsageLimit())
                .usedCount(promotion.getUsedCount())
                .usageLimitPerUser(promotion.getUsageLimitPerUser())
                .minOrderValue(promotion.getMinOrderValue())
                .status(promotion.getStatus())
                .shopId(shopId)
                .shopName(shopName)
                .expired(expired)
                .currentlyActive(currentlyActive)
                .remainingUsage(remainingUsage)
                .productIds(productIds)
                .products(productDtos)
                .createdAt(promotion.getCreatedAt())
                .updatedAt(promotion.getUpdatedAt())
                .build();
    }
}
