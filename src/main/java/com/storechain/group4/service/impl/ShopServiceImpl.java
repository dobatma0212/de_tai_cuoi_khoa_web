package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ShopRegistrationRequest;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopUpdateRequest;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "status", "createdAt"
    );

    // ==================== Người dùng (profile) ====================

    @Override
    @Transactional(readOnly = true)
    public List<ShopResponseDto> getMyShops(Long userId) {
        return shopRepository.findAllByOwnerIdOrderByIdDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopResponseDto registerShop(ShopRegistrationRequest request, Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Chống spam: mỗi user chỉ được có TỐI ĐA 1 đơn đăng ký đang chờ duyệt
        if (shopRepository.existsByOwnerIdAndStatusIgnoreCase(userId, "pending")) {
            throw new IllegalArgumentException(
                    "Bạn đang có một đơn đăng ký mở cửa hàng đang chờ duyệt. Vui lòng chờ quản trị viên xử lý trước khi gửi đơn mới.");
        }

        String name = normalizeRequired(request.getName(), "Tên cửa hàng không được để trống");
        if (shopRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên cửa hàng '" + name + "' đã tồn tại, vui lòng chọn tên khác");
        }

        Shop shop = Shop.builder()
                .owner(owner)
                .name(name)
                .description(normalizeOptional(request.getDescription()))
                .phone(normalizeOptional(request.getPhone()))
                .address(normalizeOptional(request.getAddress()))
                .logoUrl(normalizeOptional(request.getLogoUrl()))
                .bannerUrl(normalizeOptional(request.getBannerUrl()))
                .status("pending")
                .build();

        Shop saved = shopRepository.save(shop);
        log.info("Người dùng {} đã gửi đăng ký mở cửa hàng: ID={}, Name={}", userId, saved.getId(), saved.getName());
        return toDto(saved);
    }

    // ==================== Vendor ====================

    @Override
    @Transactional(readOnly = true)
    public List<ShopResponseDto> getMyActiveShops(Long userId) {
        return shopRepository.findAllByOwnerIdAndStatusIgnoreCaseOrderByIdDesc(userId, "active").stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getMyShop(Long userId, Long shopId) {
        return toDto(requireMyActiveShop(userId, shopId));
    }

    @Override
    @Transactional
    public ShopResponseDto updateMyShop(ShopUpdateRequest request, Long userId, Long shopId) {
        Shop shop = requireMyActiveShop(userId, shopId);

        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            if (shopRepository.existsByNameIgnoreCaseAndIdNot(name, shop.getId())) {
                throw new IllegalArgumentException("Tên cửa hàng '" + name + "' đã tồn tại, vui lòng chọn tên khác");
            }
            shop.setName(name);
        }
        if (request.getDescription() != null) {
            shop.setDescription(normalizeOptional(request.getDescription()));
        }
        if (request.getPhone() != null) {
            shop.setPhone(normalizeOptional(request.getPhone()));
        }
        if (request.getAddress() != null) {
            shop.setAddress(normalizeOptional(request.getAddress()));
        }
        if (request.getLogoUrl() != null) {
            shop.setLogoUrl(normalizeOptional(request.getLogoUrl()));
        }
        if (request.getBannerUrl() != null) {
            shop.setBannerUrl(normalizeOptional(request.getBannerUrl()));
        }

        Shop updated = shopRepository.save(shop);
        log.info("Đã cập nhật cửa hàng: ID={}", updated.getId());
        return toDto(updated);
    }

    // ==================== Admin ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ShopResponseDto> getShops(String keyword, String status, int page, int size, String sortBy, String sortDir) {
        if (sortBy == null || sortBy.isBlank() || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "id";
        }
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return shopRepository.searchShops(keyword, status, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với ID: " + id));
        return toDto(shop);
    }

    @Override
    @Transactional
    public ShopResponseDto approveShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với ID: " + id));

        shop.setStatus("active");
        shop.setRejectReason(null);

        // Cấp role VENDOR cho chủ shop nếu chưa có
        User owner = shop.getOwner();
        if (owner != null && !RoleType.ROLE_VENDOR.equals(owner.getRole() == null ? null : owner.getRole().getName())) {
            roleRepository.findByName(RoleType.ROLE_VENDOR).ifPresent(owner::setRole);
            userRepository.save(owner);
            log.info("Đã cấp role VENDOR cho user ID={}", owner.getId());
        }

        Shop saved = shopRepository.save(shop);
        log.info("Đã duyệt cửa hàng: ID={}, Name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ShopResponseDto rejectShop(Long id, String reason) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với ID: " + id));

        shop.setStatus("rejected");
        shop.setRejectReason(normalizeOptional(reason));
        Shop saved = shopRepository.save(shop);
        log.info("Đã từ chối cửa hàng: ID={}, Lý do={}", saved.getId(), saved.getRejectReason());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ShopResponseDto updateShopStatus(Long id, String status) {
        if (!"active".equalsIgnoreCase(status) && !"banned".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'active' hoặc 'banned'");
        }
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với ID: " + id));

        shop.setStatus(status.toLowerCase());
        shop.setRejectReason(null);
        Shop saved = shopRepository.save(shop);
        log.info("Đã cập nhật trạng thái cửa hàng ID {} thành {}", id, status);
        return toDto(saved);
    }

    // ==================== Public ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ShopResponseDto> getActiveShops(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return shopRepository.searchShops(keyword, "active", pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getPublicShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với ID: " + id));
        if (!shop.isActive()) {
            throw new IllegalArgumentException("Cửa hàng này hiện không hoạt động");
        }
        return toDto(shop);
    }

    // ==================== Helper ====================

    /**
     * Lấy shop đang hoạt động của user: ưu tiên shopId được chỉ định (phải thuộc user và đang active),
     * nếu không chỉ định thì lấy shop active mới nhất.
     */
    private Shop requireMyActiveShop(Long userId, Long shopId) {
        Shop shop;
        if (shopId != null) {
            shop = shopRepository.findByIdAndOwnerId(shopId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng này trong danh sách cửa hàng của bạn"));
        } else {
            List<Shop> actives = shopRepository.findAllByOwnerIdAndStatusIgnoreCaseOrderByIdDesc(userId, "active");
            shop = actives.isEmpty() ? null : actives.get(0);
        }
        if (shop == null) {
            throw new IllegalArgumentException("Bạn chưa có cửa hàng đang hoạt động");
        }
        if (!shop.isActive()) {
            throw new IllegalArgumentException("Cửa hàng của bạn chưa được duyệt hoặc đã bị khóa");
        }
        return shop;
    }

    private ShopResponseDto toDto(Shop shop) {
        User owner = shop.getOwner();
        return ShopResponseDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .phone(shop.getPhone())
                .address(shop.getAddress())
                .status(shop.getStatus())
                .rejectReason(shop.getRejectReason())
                .createdAt(shop.getCreatedAt())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerUsername(owner != null ? owner.getUsername() : null)
                .ownerFullName(owner != null ? owner.getFullName() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .build();
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = value != null ? value.trim() : "";
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
