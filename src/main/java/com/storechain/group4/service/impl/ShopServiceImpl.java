package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ShopDetailDto;
import com.storechain.group4.dto.ShopRequestDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopStatsDto;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.Order;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
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
    private final BranchStockRepository branchStockRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

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
    @Transactional(readOnly = true)
    public ShopDetailDto getShopDetail(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));

        YearMonth ym = YearMonth.now();
        LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = ym.plusMonths(1).atDay(1).atStartOfDay();

        BigDecimal monthRevenue = BigDecimal.ZERO;
        long monthOrders = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0L;

        if (orderRepository != null) {
            try {
                BigDecimal mr = orderRepository.sumMonthlyRevenueByShopId(id, startOfMonth, endOfMonth);
                if (mr != null) monthRevenue = mr;
                monthOrders = orderRepository.countMonthlyOrdersByShopId(id, startOfMonth, endOfMonth);
                BigDecimal tr = orderRepository.sumTotalRevenueByShopId(id);
                if (tr != null) totalRevenue = tr;
                totalOrders = orderRepository.countTotalOrdersByShopId(id);
            } catch (Exception e) {
                log.warn("Lỗi lấy chỉ số doanh thu đơn hàng chi nhánh ID {}: {}", id, e.getMessage());
            }
        }

        int totalStock = 0;
        if (branchStockRepository != null) {
            try {
                totalStock = branchStockRepository.sumStockByShopId(id);
            } catch (Exception e) {
                log.warn("Lỗi lấy tổng tồn kho từ branch_stocks cho chi nhánh ID {}: {}", id, e.getMessage());
            }
        }
        if (totalStock == 0 && productRepository != null) {
            try {
                totalStock = productRepository.sumStockByShopId(id);
            } catch (Exception e) {
                log.warn("Lỗi lấy tổng tồn kho từ products cho chi nhánh ID {}: {}", id, e.getMessage());
            }
        }

        // Danh sách sản phẩm tồn kho tại chi nhánh
        List<ShopDetailDto.BranchProductStockItemDto> productItems = new ArrayList<>();
        if (branchStockRepository != null) {
            try {
                List<BranchStock> branchStocks = branchStockRepository.findByShopIdWithProduct(id);
                for (BranchStock bs : branchStocks) {
                    if (bs.getProduct() == null) continue;
                    productItems.add(ShopDetailDto.BranchProductStockItemDto.builder()
                            .productId(bs.getProduct().getId())
                            .productName(bs.getProduct().getName())
                            .baseSku(bs.getProduct().getBaseSku())
                            .categoryName(bs.getProduct().getCategory() != null ? bs.getProduct().getCategory().getName() : "Khác")
                            .price(bs.getProduct().getPrice())
                            .imageUrl(bs.getProduct().getImageUrl())
                            .stock(bs.getStock())
                            .sizeStocks(bs.getSizeStocks())
                            .status(bs.getProduct().getStatus())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Lỗi lấy danh sách branch_stocks cho chi nhánh ID {}: {}", id, e.getMessage());
            }
        }

        // Fallback sang products liên kết trực tiếp với chi nhánh nếu branch_stocks trống
        if (productItems.isEmpty() && productRepository != null) {
            try {
                List<Product> directProducts = productRepository.findByShopIdOrderByCreatedAtDesc(id);
                for (Product p : directProducts) {
                    productItems.add(ShopDetailDto.BranchProductStockItemDto.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .baseSku(p.getBaseSku())
                            .categoryName(p.getCategory() != null ? p.getCategory().getName() : "Khác")
                            .price(p.getPrice())
                            .imageUrl(p.getImageUrl())
                            .stock(p.getStock())
                            .sizeStocks(p.getSizes())
                            .status(p.getStatus())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Lỗi lấy danh sách products trực tiếp cho chi nhánh ID {}: {}", id, e.getMessage());
            }
        }

        // Danh sách đơn hàng gần đây của chi nhánh
        List<ShopDetailDto.BranchRecentOrderItemDto> recentOrders = new ArrayList<>();
        if (orderRepository != null) {
            try {
                List<Order> orders = orderRepository.findRecentOrdersByShopId(id, PageRequest.of(0, 10));
                for (Order o : orders) {
                    String statusLabel = switch (o.getStatus() == null ? "" : o.getStatus()) {
                        case Order.STATUS_NEW -> "Đơn mới";
                        case Order.STATUS_CONFIRMED -> "Đã xác nhận";
                        case Order.STATUS_PICKED_UP -> "Đã lấy hàng";
                        case Order.STATUS_SHIPPING -> "Đang giao";
                        case Order.STATUS_DELIVERED -> "Đã giao";
                        case Order.STATUS_RECEIVED -> "Đã nhận hàng";
                        case Order.STATUS_CANCELLED -> "Đã hủy";
                        case Order.STATUS_RETURNED -> "Trả hàng";
                        case Order.STATUS_RETURN_REQUESTED -> "Yêu cầu trả";
                        default -> o.getStatus() != null ? o.getStatus() : "Chờ xử lý";
                    };

                    String customerName = o.getReceiverName();
                    if ((customerName == null || customerName.isBlank()) && o.getUser() != null) {
                        customerName = o.getUser().getFullName() != null ? o.getUser().getFullName() : o.getUser().getUsername();
                    }

                    recentOrders.add(ShopDetailDto.BranchRecentOrderItemDto.builder()
                            .orderId(o.getId())
                            .orderCode(o.getOrderCode())
                            .customerName(customerName != null ? customerName : "Khách lẻ")
                            .customerPhone(o.getReceiverPhone())
                            .grandTotal(o.getGrandTotal() != null ? o.getGrandTotal() : BigDecimal.ZERO)
                            .status(o.getStatus())
                            .statusLabel(statusLabel)
                            .orderType(o.getOrderType())
                            .paymentMethod(o.getPaymentMethod())
                            .paymentStatus(o.getPaymentStatus())
                            .createdAt(o.getCreatedAt())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Lỗi lấy đơn hàng gần đây của chi nhánh ID {}: {}", id, e.getMessage());
            }
        }

        // Danh sách nhân viên làm việc trong chi nhánh
        List<ShopDetailDto.BranchEmployeeItemDto> employeeItems = new ArrayList<>();
        long activeEmployeeCount = 0L;
        if (userRepository != null) {
            try {
                List<User> employees = userRepository.findByShopId(id);
                for (User emp : employees) {
                    boolean isEmployed = emp.getRole() != null && RoleType.ROLE_EMPLOYEE.equals(emp.getRole().getName());
                    // Chỉ hiển thị nhân viên đang làm việc trong chi nhánh (không hiển thị người đã bị sa thải / tài khoản đã xóa / role khác)
                    if (!isEmployed) {
                        continue;
                    }
                    activeEmployeeCount++;
                    String roleDesc = emp.getRole() != null && emp.getRole().getDescription() != null && !emp.getRole().getDescription().isBlank()
                            ? emp.getRole().getDescription()
                            : "Nhân viên làm việc tại chi nhánh";
                    employeeItems.add(ShopDetailDto.BranchEmployeeItemDto.builder()
                            .id(emp.getId())
                            .username(emp.getUsername())
                            .fullName(emp.getFullName())
                            .email(emp.getEmail())
                            .phone(emp.getPhone())
                            .avatarUrl(emp.getAvatarUrl())
                            .status(emp.getStatus())
                            .employed(true)
                            .roleName("EMPLOYEE")
                            .roleDescription(roleDesc)
                            .createdAt(emp.getCreatedAt())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Lỗi lấy danh sách nhân viên chi nhánh ID {}: {}", id, e.getMessage());
            }
        }

        ShopDetailDto detail = ShopDetailDto.builder()
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
                .currentMonthRevenue(monthRevenue)
                .totalStock(totalStock)
                .totalProducts((long) productItems.size())
                .currentMonthOrders(monthOrders)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalEmployees(activeEmployeeCount)
                .products(productItems)
                .recentOrders(recentOrders)
                .employees(employeeItems)
                .build();

        if (shop.getManager() != null) {
            detail.setManagerId(shop.getManager().getId());
            detail.setManagerUsername(shop.getManager().getUsername());
            detail.setManagerFullName(shop.getManager().getFullName());
            detail.setManagerEmail(shop.getManager().getEmail());
            detail.setManagerPhone(shop.getManager().getPhone());
        }

        return detail;
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

        // Dọn dẹp tồn kho chi nhánh
        try {
            if (branchStockRepository != null) {
                branchStockRepository.deleteByShopId(id);
            }
        } catch (Exception e) {
            log.warn("Lỗi dọn dẹp branch_stocks của chi nhánh ID {}: {}", id, e.getMessage());
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
                .currentMonthRevenue(BigDecimal.ZERO)
                .totalStock(0)
                .totalProducts(0L)
                .currentMonthOrders(0L)
                .build();

        if (shop.getManager() != null) {
            dto.setManagerId(shop.getManager().getId());
            dto.setManagerUsername(shop.getManager().getUsername());
            dto.setManagerFullName(shop.getManager().getFullName());
            dto.setManagerEmail(shop.getManager().getEmail());
            dto.setManagerPhone(shop.getManager().getPhone());
        }

        // Bổ sung chỉ số tóm tắt nếu có shop ID
        if (shop.getId() != null) {
            try {
                if (orderRepository != null) {
                    YearMonth ym = YearMonth.now();
                    LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
                    LocalDateTime endOfMonth = ym.plusMonths(1).atDay(1).atStartOfDay();
                    BigDecimal mRev = orderRepository.sumMonthlyRevenueByShopId(shop.getId(), startOfMonth, endOfMonth);
                    if (mRev != null) dto.setCurrentMonthRevenue(mRev);
                    dto.setCurrentMonthOrders(orderRepository.countMonthlyOrdersByShopId(shop.getId(), startOfMonth, endOfMonth));
                }
                if (branchStockRepository != null) {
                    int bStock = branchStockRepository.sumStockByShopId(shop.getId());
                    if (bStock == 0 && productRepository != null) {
                        int pStock = productRepository.sumStockByShopId(shop.getId());
                        bStock = Math.max(bStock, pStock);
                    }
                    dto.setTotalStock(bStock);
                }
                if (productRepository != null) {
                    dto.setTotalProducts(productRepository.countByShopId(shop.getId()));
                }
                if (userRepository != null) {
                    dto.setTotalEmployees(userRepository.countEmployeesByShopId(shop.getId()));
                }
            } catch (Exception e) {
                log.debug("Lỗi lấy chỉ số tóm tắt cho chi nhánh #{}: {}", shop.getId(), e.getMessage());
            }
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
