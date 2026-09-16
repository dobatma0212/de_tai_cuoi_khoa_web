package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ShopRequestDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopStatsDto;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.ShopService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern COMBINING_DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    @Transactional(readOnly = true)
    public Page<ShopResponseDto> getShops(
            String keyword,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id";
        }
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return shopRepository.searchShops(keyword, status, pageable)
                .map(this::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopStatsDto getStats() {
        long total = shopRepository.count();
        long active = shopRepository.countByStatus("active");
        long inactive = shopRepository.countByStatus("inactive");
        long assigned = shopRepository.countByManagerIsNotNull();

        return ShopStatsDto.builder()
                .totalShops(total)
                .activeShops(active)
                .inactiveShops(inactive)
                .assignedShops(assigned)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));
        return toResponseDto(shop);
    }

    @Override
    @Transactional
    public ShopResponseDto createShop(ShopRequestDto request) {
        String trimmedName = request.getName().trim();
        if (shopRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Tên chi nhánh '" + trimmedName + "' đã tồn tại trong hệ thống");
        }

        User manager = null;
        if (request.getManagerId() != null && request.getManagerId() > 0) {
            manager = validateAndGetManager(request.getManagerId());
        }

        Shop shop = Shop.builder()
                .name(trimmedName)
                .slug(toSlug(trimmedName))
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus().trim() : "active")
                .manager(manager)
                .build();

        Shop saved = shopRepository.save(shop);
        log.info("Tạo chi nhánh mới thành công: ID {}, Tên: {}", saved.getId(), saved.getName());
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    public ShopResponseDto updateShop(Long id, ShopRequestDto request) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));

        String trimmedName = request.getName().trim();
        if (shopRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new IllegalArgumentException("Tên chi nhánh '" + trimmedName + "' đã tồn tại");
        }

        shop.setName(trimmedName);
        shop.setSlug(toSlug(trimmedName));
        shop.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        shop.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        shop.setDescription(request.getDescription());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            shop.setStatus(request.getStatus().trim());
        }

        if (request.getManagerId() != null) {
            if (request.getManagerId() <= 0) {
                shop.setManager(null);
            } else {
                User manager = validateAndGetManager(request.getManagerId());
                shop.setManager(manager);
            }
        }

        Shop updated = shopRepository.save(shop);
        log.info("Cập nhật chi nhánh ID {} thành công", id);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public ShopResponseDto updateStatus(Long id, String status) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));

        if (!"active".equalsIgnoreCase(status) && !"inactive".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'active' hoặc 'inactive'");
        }

        shop.setStatus(status.toLowerCase());
        Shop updated = shopRepository.save(shop);
        log.info("Cập nhật trạng thái chi nhánh ID {} sang '{}'", id, status);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public ShopResponseDto assignManager(Long shopId, Long managerId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + shopId));

        if (managerId == null || managerId <= 0) {
            shop.setManager(null);
            log.info("Đã gỡ bỏ Quản lý của chi nhánh ID {}", shopId);
        } else {
            User manager = validateAndGetManager(managerId);
            shop.setManager(manager);
            log.info("Đã bổ nhiệm Quản lý {} (ID: {}) cho chi nhánh ID {}", manager.getFullName(), manager.getId(), shopId);
        }

        Shop updated = shopRepository.save(shop);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));

        // Dọn dẹp an toàn các sản phẩm thuộc chi nhánh trước khi xóa chi nhánh
        try {
            int deletedProducts = entityManager.createNativeQuery("DELETE FROM products WHERE shop_id = :shopId")
                    .setParameter("shopId", id)
                    .executeUpdate();
            if (deletedProducts > 0) {
                log.info("Đã xóa {} sản phẩm liên kết với chi nhánh ID {}", deletedProducts, id);
            }
        } catch (Exception e) {
            log.warn("Lỗi dọn dẹp sản phẩm liên kết với chi nhánh ID {}: {}", id, e.getMessage());
        }

        shopRepository.delete(shop);
        log.info("Đã xóa chi nhánh ID {} thành công", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAvailableManagers() {
        return userRepository.findByRoleType(RoleType.ROLE_MANAGER).stream()
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .avatarUrl(user.getAvatarUrl())
                        .status(user.getStatus())
                        .role(user.getRole() != null ? user.getRole().getName() : null)
                        .roleName(user.getRole() != null ? user.getRole().getName().name() : "N/A")
                        .roleDescription(user.getRole() != null ? user.getRole().getDescription() : "")
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private User validateAndGetManager(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với ID: " + managerId));

        if (manager.getRole() == null || manager.getRole().getName() != RoleType.ROLE_MANAGER) {
            throw new IllegalArgumentException("Người dùng '" + manager.getUsername() + "' không có vai trò Quản lý (ROLE_MANAGER)");
        }
        return manager;
    }

    private ShopResponseDto toResponseDto(Shop shop) {
        ShopResponseDto dto = ShopResponseDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .address(shop.getAddress())
                .phone(shop.getPhone())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .status(shop.getStatus())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();

        if (shop.getManager() != null) {
            dto.setManagerId(shop.getManager().getId());
            dto.setManagerUsername(shop.getManager().getUsername());
            dto.setManagerFullName(shop.getManager().getFullName());
            dto.setManagerEmail(shop.getManager().getEmail());
            dto.setManagerPhone(shop.getManager().getPhone());
        }

        return dto;
    }

    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = COMBINING_DIACRITICS.matcher(normalized).replaceAll("");
        withoutDiacritics = withoutDiacritics.replace('đ', 'd').replace('Đ', 'D');

        String nowhitespace = WHITESPACE.matcher(withoutDiacritics).replaceAll("-");
        String normalizedSlug = NON_LATIN.matcher(nowhitespace).replaceAll("");
        return normalizedSlug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
