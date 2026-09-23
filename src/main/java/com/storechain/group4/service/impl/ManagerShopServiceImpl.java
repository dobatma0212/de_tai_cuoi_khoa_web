package com.storechain.group4.service.impl;

import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.PromotionDto;
import com.storechain.group4.dto.RevenueDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.Order;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Promotion;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.ShopCategory;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.ShopCategoryRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.service.ManagerShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerShopServiceImpl implements ManagerShopService {

    private final ShopRepository shopRepository;
    private final ShopCategoryRepository shopCategoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PromotionRepository promotionRepository;
    private final BranchStockRepository branchStockRepository;
    private final com.storechain.group4.repository.TransporterRepository transporterRepository;
    private final com.storechain.group4.repository.CartItemRepository cartItemRepository;
    private final com.storechain.group4.repository.ProductReviewRepository reviewRepository;
    private final com.storechain.group4.repository.ReviewMediaRepository reviewMediaRepository;
    private final com.storechain.group4.repository.UserRepository userRepository;
    private final com.storechain.group4.repository.RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * Dùng để tính thống kê công/hoa hồng của nhân viên. Inject @Lazy (không qua
     * constructor) vì EmployeeServiceImpl cũng cần ManagerShopService (upload ảnh) —
     * tránh vòng phụ thuộc bean.
     */
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private com.storechain.group4.service.EmployeeService employeeService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Số phút đơn đã xác nhận chuyển sang Đang giao (mặc định 2 phút) */
    @Value("${app.order.shipping-delay-minutes:2}")
    private long shippingDelayMinutes;

    /** Số phút đơn Đang giao chuyển sang Đã giao (mặc định 5 phút) */
    @Value("${app.order.deliver-delay-minutes:5}")
    private long deliverDelayMinutes;

    private static final String DEFAULT_CATEGORY_CLOTHING = "Bộ áo quần";
    private static final String DEFAULT_CATEGORY_ACCESSORY = "Phụ kiện";

    // ============ Chi nhánh của manager ============

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getMyShop(Long managerId) {
        Shop shop = findMyShop(managerId);
        return toShopDto(shop);
    }

    // Thông tin chi nhánh (tên, địa chỉ, SĐT, mô tả) do admin cập nhật, manager chỉ được xem

    private Shop findMyShop(Long managerId) {
        List<Shop> shops = shopRepository.findByManagerId(managerId);
        if (shops.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn chưa được admin bổ nhiệm phụ trách chi nhánh nào. Vui lòng liên hệ quản trị viên.");
        }
        return shops.get(0);
    }

    // ============ Danh mục sản phẩm ============

    @Override
    @Transactional
    public List<ShopCategoryDto> getMyCategories(Long managerId) {
        Shop shop = findMyShop(managerId);
        ensureDefaultCategories(shop);
        return toCategoryDtos(shop.getId());
    }

    // Manager chỉ được xem danh mục; thêm/sửa/xóa do admin và nhân viên thực hiện

    /** Chi nhánh mới chưa có danh mục thì tạo sẵn 2 danh mục mặc định */
    private void ensureDefaultCategories(Shop shop) {
        if (shopCategoryRepository.countByShopId(shop.getId()) > 0) {
            return;
        }
        shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop).name(DEFAULT_CATEGORY_CLOTHING).sortOrder(0).build());
        shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop).name(DEFAULT_CATEGORY_ACCESSORY).sortOrder(1).build());
        log.info("Đã tạo danh mục mặc định cho chi nhánh ID: {}", shop.getId());
    }

    private List<ShopCategoryDto> toCategoryDtos(Long shopId) {
        return shopCategoryRepository.findByShopIdOrderBySortOrderAscIdAsc(shopId)
                .stream().map(this::toCategoryDto).toList();
    }

    private ShopCategoryDto toCategoryDto(ShopCategory c) {
        return ShopCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .sortOrder(c.getSortOrder())
                .totalProducts(productRepository.countByShopIdAndCategoryId(c.getShop().getId(), c.getId()))
                .build();
    }

    // ============ Sản phẩm chi nhánh (manager chỉ được xem) ============

    @Override
    @Transactional
    public List<ProductResponseDto> getMyProducts(Long managerId, Long categoryId) {
        Shop shop = findMyShop(managerId);
        ensureDefaultCategories(shop);
        List<Product> products = (categoryId == null || categoryId <= 0)
                ? productRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                : productRepository.findByShopIdAndCategoryIdOrderByCreatedAtDesc(shop.getId(), categoryId);
        return products.stream().map(this::toProductDto).toList();
    }

    // Thêm/sửa/xóa sản phẩm do nhân viên thực hiện (EmployeeServiceImpl), manager chỉ được xem

    // ============ Upload ảnh ============

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp hình ảnh để tải lên.");
        }
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase(Locale.ROOT);
        }
        List<String> allowed = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận hình ảnh JPG, PNG, GIF, WEBP.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước ảnh tối đa là 5MB.");
        }
        try {
            Path dir = Paths.get(uploadDir, "products");
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/products/" + filename;
        } catch (IOException e) {
            log.error("Lỗi lưu tệp ảnh tải lên: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lưu tệp ảnh, vui lòng thử lại.");
        }
    }

    // ============ Quản lý đơn hàng của chi nhánh ============

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrders(Long managerId, String status) {
        Shop shop = findMyShop(managerId);
        autoAdvanceStaleOrders();
        List<Order> orders = (status == null || status.isBlank())
                ? orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                : orderRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shop.getId(), status);
        return orders.stream().map(this::toOrderDto).toList();
    }

    /**
     * Manager chỉ được:
     * - CONFIRMED: xác nhận đơn NEW (dùng đơn vị vận chuyển khách đã chọn khi đặt hàng), giao hàng tự động sau đó
     * - CANCELLED: hủy đơn NEW/CONFIRMED/SHIPPING (bắt buộc ghi lý do), hoàn tồn kho về chi nhánh
     * - RETURNED: đánh dấu trả hàng - hoàn tiền cho đơn đã giao/khách đã nhận
     * Trạng thái SHIPPING/DELIVERED/RECEIVED do hệ thống và khách hàng tự cập nhật.
     */
    @Override
    @Transactional
    public OrderDto updateOrderStatus(Long managerId, Long orderId, String status,
                                      Long transporterId, String reason) {
        Shop shop = findMyShop(managerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc chi nhánh bạn quản lý.");
        }
        String current = order.getStatus();
        switch (status == null ? "" : status) {
            case Order.STATUS_CONFIRMED -> {
                if (!Order.STATUS_NEW.equals(current)) {
                    throw new IllegalArgumentException("Chỉ đơn ở trạng thái mới mới có thể xác nhận.");
                }
                // Đơn vị vận chuyển do khách chọn lúc đặt hàng đã lưu trên đơn — giữ nguyên,
                // chỉ tự điền khi đơn cũ chưa có (fallback theo transporterId gửi lên)
                if (order.getTransporter() == null && transporterId != null) {
                    var transporter = transporterRepository.findById(transporterId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển."));
                    order.setTransporter(transporter);
                }
                if (order.getTransporter() == null) {
                    throw new IllegalArgumentException("Đơn hàng chưa có đơn vị vận chuyển để xác nhận.");
                }
                if (order.getShippingFee() == null || order.getShippingFee().compareTo(BigDecimal.ZERO) <= 0) {
                    order.setShippingFee(order.getTransporter().getBaseFee() == null
                            ? BigDecimal.ZERO : order.getTransporter().getBaseFee());
                    order.setGrandTotal(order.getTotalAmount()
                            .add(order.getShippingFee())
                            .subtract(order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount()));
                }
                order.setStatus(Order.STATUS_CONFIRMED);
            }
            case Order.STATUS_CANCELLED -> {
                if (!List.of(Order.STATUS_NEW, Order.STATUS_CONFIRMED, Order.STATUS_SHIPPING).contains(current)) {
                    throw new IllegalArgumentException("Không thể hủy đơn ở trạng thái hiện tại.");
                }
                if (reason == null || reason.isBlank()) {
                    throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn (VD: tồn kho chi nhánh đã hết).");
                }
                order.setCancelReason(reason.trim());
                order.setStatus(Order.STATUS_CANCELLED);
                restockOrder(order);
            }
            case Order.STATUS_RETURNED -> {
                if (!List.of(Order.STATUS_SHIPPING, Order.STATUS_DELIVERED,
                        Order.STATUS_RECEIVED, Order.STATUS_RETURN_REQUESTED).contains(current)) {
                    throw new IllegalArgumentException("Chỉ đơn đang giao/đã giao/đã nhận/yêu cầu trả hàng mới duyệt trả được.");
                }
                if (reason != null && !reason.isBlank()) {
                    order.setCancelReason(reason.trim());
                }
                order.setStatus(Order.STATUS_RETURNED);
                restockOrder(order);
            }
            default -> throw new IllegalArgumentException(
                    "Bạn chỉ có thể xác nhận, hủy (kèm lý do) hoặc đánh dấu trả hàng cho đơn.");
        }
        return toOrderDto(orderRepository.save(order));
    }

    /** Hoàn lại tồn kho các sản phẩm của đơn bị hủy về chi nhánh */
    private void restockOrder(Order order) {
        for (var oi : orderItemRepository.findByOrderId(order.getId())) {
            if (oi.getProduct() == null) continue;
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(oi.getProduct().getId(), order.getShop().getId())
                    .orElse(BranchStock.builder().product(oi.getProduct())
                            .shop(order.getShop()).stock(0).build());
            bs.setStock(bs.getStock() + oi.getQuantity());
            branchStockRepository.save(bs);
        }
    }

    // ============ Chương trình khuyến mãi ============

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDto> getPromotions(Long managerId) {
        Shop shop = findMyShop(managerId);
        return promotionRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                .stream().map(this::toPromotionDto).toList();
    }

    @Override
    @Transactional
    public PromotionDto savePromotion(Long managerId, PromotionDto request) {
        Shop shop = findMyShop(managerId);
        Promotion promo;
        if (request.getId() != null && request.getId() > 0) {
            promo = promotionRepository.findByIdAndShopId(request.getId(), shop.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chương trình khuyến mãi."));
        } else {
            promo = Promotion.builder().shop(shop).build();
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên khuyến mãi không được để trống.");
        }
        if (!Promotion.TYPE_PERCENT.equals(request.getPromoType())
                && !Promotion.TYPE_SHIPPING.equals(request.getPromoType())) {
            throw new IllegalArgumentException("Loại khuyến mãi phải là PERCENT (giảm %) hoặc SHIPPING (giảm phí vận chuyển).");
        }
        if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị khuyến mãi phải lớn hơn 0.");
        }
        if (Promotion.TYPE_PERCENT.equals(request.getPromoType())
                && request.getValue().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("Giảm giá theo % tối đa là 90%.");
        }
        String code = request.getCode() == null || request.getCode().isBlank()
                ? null : request.getCode().trim();
        if (code != null && promotionRepository
                .existsByShopIdAndCodeIgnoreCaseAndIdNot(shop.getId(), code,
                        request.getId() != null ? request.getId() : 0L)) {
            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại trong chi nhánh.");
        }
        promo.setName(request.getName().trim());
        promo.setCode(code);
        promo.setPromoType(request.getPromoType());
        // Ánh xạ sang mô hình chung: PERCENT = giảm % giá đơn, SHIPPING = giảm tiền cố định trên phí vận chuyển
        if (Promotion.TYPE_PERCENT.equals(request.getPromoType())) {
            promo.setDiscountType(com.storechain.group4.model.DiscountType.PERCENTAGE);
        } else {
            promo.setDiscountType(com.storechain.group4.model.DiscountType.FIXED_AMOUNT);
        }
        promo.setPromotionType(com.storechain.group4.model.PromotionType.VOUCHER);
        promo.setDiscountValue(request.getValue());
        promo.setMinOrderValue(request.getMinOrderAmount() == null
                ? BigDecimal.ZERO : request.getMinOrderAmount());
        promo.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
        promo.setEndDate(request.getEndDate());
        promo.setActive(request.isActive());
        return toPromotionDto(promotionRepository.save(promo));
    }

    @Override
    @Transactional
    public void deletePromotion(Long managerId, Long promoId) {
        Shop shop = findMyShop(managerId);
        Promotion promo = promotionRepository.findByIdAndShopId(promoId, shop.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chương trình khuyến mãi."));
        promotionRepository.delete(promo);
    }

    // ============ Doanh thu chi nhánh ============

    @Override
    @Transactional(readOnly = true)
    public RevenueDto getRevenue(Long managerId) {
        Shop shop = findMyShop(managerId);
        List<Order> orders = orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());

        BigDecimal revenue = BigDecimal.ZERO;      // tổng tiền khách đã trả cho đơn DELIVERED
        BigDecimal productAmount = BigDecimal.ZERO;
        BigDecimal shippingTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        long delivered = 0, cancelled = 0, returned = 0;
        Map<String, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus() == null ? "" : o.getStatus(),
                        Collectors.counting()));
        BigDecimal shippingLoss = BigDecimal.ZERO; // phí VC mất đi cho đơn hủy/trả hàng

        for (Order o : orders) {
            // Đơn thành công = đã giao HOẶC khách đã xác nhận nhận hàng
            if (Order.STATUS_DELIVERED.equals(o.getStatus()) || Order.STATUS_RECEIVED.equals(o.getStatus())) {
                delivered++;
                revenue = revenue.add(o.getGrandTotal() == null ? BigDecimal.ZERO : o.getGrandTotal());
                productAmount = productAmount.add(o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount());
                shippingTotal = shippingTotal.add(o.getShippingFee() == null ? BigDecimal.ZERO : o.getShippingFee());
                discountTotal = discountTotal.add(o.getDiscountAmount() == null ? BigDecimal.ZERO : o.getDiscountAmount());
            } else if (Order.STATUS_CANCELLED.equals(o.getStatus())) {
                cancelled++;
                // Đơn đã chuyển đi rồi mới hủy/trả thì chi nhánh vẫn mất phí vận chuyển
                if (o.getTransporter() != null) {
                    shippingLoss = shippingLoss.add(shippingCostOf(o));
                }
            } else if (Order.STATUS_RETURNED.equals(o.getStatus())) {
                returned++;
                shippingLoss = shippingLoss.add(shippingCostOf(o));
            }
        }
        // Doanh thu thực nhận = tiền các đơn đã giao trừ phí VC của đơn hủy/trả (có thể âm)
        revenue = revenue.subtract(shippingLoss);
        return RevenueDto.builder()
                .shippingLoss(shippingLoss)
                .totalRevenue(revenue)
                .deliveredProductAmount(productAmount)
                .deliveredShippingFee(shippingTotal)
                .deliveredDiscount(discountTotal)
                .totalOrders(orders.size())
                .deliveredOrders(delivered)
                .cancelledOrders(cancelled)
                .returnedOrders(returned)
                .statusCounts(statusCounts)
                .build();
    }

    /**
     * Đẩy mốc giao hàng tự động theo thời gian, chạy mỗi lần mở danh sách đơn
     * (không phụ thuộc job nền): Đã xác nhận quá 10s -> Đang giao, quá 20s -> Đã giao.
     */
    private void autoAdvanceStaleOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> toSave = new java.util.ArrayList<>();
        orderRepository.findByStatus(Order.STATUS_CONFIRMED).stream()
                .filter(o -> o.getUpdatedAt() != null
                        && o.getUpdatedAt().plusMinutes(shippingDelayMinutes).isBefore(now))
                .peek(o -> o.setStatus(Order.STATUS_SHIPPING))
                .forEach(toSave::add);
        orderRepository.findByStatus(Order.STATUS_SHIPPING).stream()
                .filter(o -> o.getUpdatedAt() != null
                        && o.getUpdatedAt().plusMinutes(deliverDelayMinutes).isBefore(now))
                .peek(o -> o.setStatus(Order.STATUS_DELIVERED))
                .forEach(toSave::add);
        if (!toSave.isEmpty()) {
            orderRepository.saveAll(toSave);
        }
    }

    /** Phí VC phải trả cho đơn hủy/trả: lấy phí đã thu, chưa có thì lấy phí của đơn vị vận chuyển */
    private BigDecimal shippingCostOf(Order o) {
        if (o.getShippingFee() != null && o.getShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            return o.getShippingFee();
        }
        return o.getTransporter() != null && o.getTransporter().getBaseFee() != null
                ? o.getTransporter().getBaseFee()
                : BigDecimal.ZERO;
    }

    // ============ Quản lý nhân viên chi nhánh ============

    @Override
    @Transactional(readOnly = true)
    public List<com.storechain.group4.dto.EmployeeDto> getMyEmployees(Long managerId) {
        Shop shop = findMyShop(managerId);
        // Bao gồm cả nhân viên đã sa thải (role USER nhưng vẫn gắn với chi nhánh)
        // để quản lý tra cứu lịch sử công việc và hoa hồng
        return userRepository.findByShopIdAndRoleNameIn(shop.getId(),
                        java.util.List.of(RoleType.ROLE_EMPLOYEE, RoleType.ROLE_USER)).stream()
                .map(this::toEmployeeDto)
                .toList();
    }

    @Override
    @Transactional
    public com.storechain.group4.dto.EmployeeDto hireEmployee(Long managerId,
            com.storechain.group4.dto.HireEmployeeRequest request) {
        Shop shop = findMyShop(managerId);
        Role employeeRole = roleRepository.findByName(RoleType.ROLE_EMPLOYEE)
                .orElseThrow(() -> new IllegalArgumentException("Role EMPLOYEE chưa được khởi tạo."));

        User employee;
        if (request.getUserId() != null) {
            // Bổ nhiệm người dùng hiện có: chỉ ROLE_USER mới được thăng làm nhân viên
            employee = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID này."));
            if (employee.getRole() == null
                    || !RoleType.ROLE_USER.equals(employee.getRole().getName())) {
                throw new IllegalArgumentException(
                        "Chỉ có thể bổ nhiệm người dùng thông thường (USER) làm nhân viên.");
            }
            if (employee.getShop() != null && !shop.getId().equals(employee.getShop().getId())) {
                throw new IllegalArgumentException("Người dùng này đã thuộc một chi nhánh khác.");
            }
            if (RoleType.ROLE_EMPLOYEE.equals(employee.getRole() == null ? null : employee.getRole().getName())) {
                throw new IllegalArgumentException("Người này hiện đã là nhân viên của một chi nhánh.");
            }
            // Người dùng từng là nhân viên của chi nhánh này (đã sa thải) được thuê lại
            employee.setRole(employeeRole);
            employee.setShop(shop);
        } else {
            // Tạo tài khoản nhân viên mới
            String username = request.getUsername() == null ? "" : request.getUsername().trim();
            String email = request.getEmail() == null ? "" : request.getEmail().trim();
            String fullName = request.getFullName() == null ? "" : request.getFullName().trim();
            String password = request.getPassword() == null ? "" : request.getPassword();
            if (username.isEmpty() || email.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException(
                        "Vui lòng nhập đủ tên đăng nhập, email, họ tên và mật khẩu để tạo tài khoản nhân viên.");
            }
            if (password.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu nhân viên tối thiểu 6 ký tự.");
            }
            if (userRepository.existsByUsername(username)) {
                // Username đã tồn tại: coi như bổ nhiệm người dùng hiện có
                employee = userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Không tìm thấy người dùng \"" + username + "\"."));
                if (employee.getRole() == null
                        || !RoleType.ROLE_USER.equals(employee.getRole().getName())) {
                    throw new IllegalArgumentException(
                            "Người dùng \"" + username + "\" không phải khách hàng nên không thể bổ nhiệm làm nhân viên.");
                }
                if (employee.getShop() != null && !shop.getId().equals(employee.getShop().getId())) {
                    throw new IllegalArgumentException("Người dùng này đã thuộc một chi nhánh khác.");
                }
                employee.setRole(employeeRole);
                employee.setShop(shop);
            } else {
                if (userRepository.existsByEmail(email)) {
                    throw new IllegalArgumentException("Email \"" + email + "\" đã được sử dụng.");
                }
                employee = User.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .fullName(fullName)
                        .phone(request.getPhone() == null ? null : request.getPhone().trim())
                        .role(employeeRole)
                        .shop(shop)
                        .build();
            }
        }
        return toEmployeeDto(userRepository.save(employee));
    }

    @Override
    @Transactional
    public void fireEmployee(Long managerId, Long employeeId) {
        Shop shop = findMyShop(managerId);
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        if (employee.getRole() == null || !RoleType.ROLE_EMPLOYEE.equals(employee.getRole().getName())
                || employee.getShop() == null || !shop.getId().equals(employee.getShop().getId())) {
            throw new IllegalArgumentException("Người này không phải nhân viên của chi nhánh bạn.");
        }
        // Sa thải = trả về quyền khách hàng (không xóa tài khoản).
        // Vẫn giữ gắn với chi nhánh để quản lý tra cứu được lịch sử công/hoa hồng
        // của nhân viên đã sa thải (getMyEmployees lọc theo role nên họ không
        // xuất hiện trong danh sách "đang làm việc" nữa).
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalArgumentException("Role USER chưa được khởi tạo."));
        employee.setRole(userRole);
        userRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public com.storechain.group4.dto.EmployeeStatsDto getEmployeeStats(Long managerId,
            Long employeeId, String month) {
        Shop shop = findMyShop(managerId);
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên."));
        // Nhân viên đang làm việc (ROLE_EMPLOYEE) hoặc đã sa thải (ROLE_USER)
        // nhưng từng thuộc chi nhánh của quản lý
        RoleType role = employee.getRole() == null ? null : employee.getRole().getName();
        boolean belongsToShop = employee.getShop() != null
                && shop.getId().equals(employee.getShop().getId());
        boolean isStaffOrFormer = RoleType.ROLE_EMPLOYEE.equals(role)
                || RoleType.ROLE_USER.equals(role);
        if (!belongsToShop || !isStaffOrFormer) {
            throw new IllegalArgumentException("Người này không phải nhân viên (hiện tại hoặc cũ) của chi nhánh bạn.");
        }
        return employeeService.getMyStats(employeeId, month);
    }

    private com.storechain.group4.dto.EmployeeDto toEmployeeDto(User u) {
        return com.storechain.group4.dto.EmployeeDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .status(u.getStatus())
                .employed(u.getRole() != null && RoleType.ROLE_EMPLOYEE.equals(u.getRole().getName()))
                .createdAt(u.getCreatedAt())
                .build();
    }

    // ============ Mapper ============

    /** "S,M,L" -> ["S","M","L"] */
    private List<String> parseSizes(String sizes) {
        if (sizes == null || sizes.isBlank()) return List.of();
        return Arrays.stream(sizes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private ShopResponseDto toShopDto(Shop shop) {
        return ShopResponseDto.builder()
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
                .managerId(shop.getManager() != null ? shop.getManager().getId() : null)
                .managerUsername(shop.getManager() != null ? shop.getManager().getUsername() : null)
                .managerFullName(shop.getManager() != null ? shop.getManager().getFullName() : null)
                .build();
    }

    private ProductResponseDto toProductDto(Product p) {
        // Tồn kho hiển thị cho manager là tồn kho thực tế tại chi nhánh
        // (đơn bị hủy/trả hoàn về branch_stocks chứ không cộng lại vào products.stock)
        Integer branchStock = branchStockRepository
                .findByProductIdAndShopId(p.getId(), p.getShop().getId())
                .map(BranchStock::getStock)
                .orElse(p.getStock());
        return ProductResponseDto.builder()
                .id(p.getId())
                .shopId(p.getShop().getId())
                .shopName(p.getShop().getName())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .name(p.getName())
                .price(p.getPrice())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .stock(p.getStock())
                .branchStock(branchStock)
                .sizes(p.getSizes())
                .sizesList(parseSizes(p.getSizes()))
                .color(p.getColor())
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private OrderDto toOrderDto(Order o) {
        List<OrderDto.ItemDto> items = orderItemRepository.findByOrderId(o.getId())
                .stream().map(oi -> OrderDto.ItemDto.builder()
                        .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                        .productName(oi.getProductName())
                        .imageUrl(oi.getImageUrl())
                        .price(oi.getPrice())
                        .quantity(oi.getQuantity())
                        .size(oi.getSize())
                        .color(oi.getColor())
                        .subtotal(oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                        .build())
                .collect(Collectors.toList());
        return OrderDto.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .shopId(o.getShop().getId())
                .shopName(o.getShop().getName())
                .userId(o.getUser().getId())
                .customerName(o.getUser().getFullName())
                .status(o.getStatus())
                .statusLabel(statusLabel(o.getStatus()))
                .totalAmount(o.getTotalAmount())
                .shippingFee(o.getShippingFee())
                .discountAmount(o.getDiscountAmount())
                .grandTotal(o.getGrandTotal())
                .promoName(o.getPromoName())
                .transporterName(o.getTransporter() != null ? o.getTransporter().getName() : null)
                .transporterId(o.getTransporter() != null ? o.getTransporter().getId() : null)
                .cancelReason(o.getCancelReason())
                .receiverName(o.getReceiverName())
                .receiverPhone(o.getReceiverPhone())
                .receiverAddress(o.getReceiverAddress())
                .note(o.getNote())
                .createdAt(o.getCreatedAt())
                .items(items)
                .build();
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case Order.STATUS_NEW -> "Đơn hàng mới";
            case Order.STATUS_CONFIRMED -> "Đã xác nhận";
            case Order.STATUS_PICKED_UP -> "Đã lấy hàng";
            case Order.STATUS_SHIPPING -> "Đang giao";
            case Order.STATUS_DELIVERED -> "Đã giao";
            case Order.STATUS_RECEIVED -> "Đã nhận hàng";
            case Order.STATUS_CANCELLED -> "Hủy";
            case Order.STATUS_RETURNED -> "Trả hàng - hoàn tiền";
            case Order.STATUS_RETURN_REQUESTED -> "Yêu cầu trả hàng";
            default -> status;
        };
    }

    private PromotionDto toPromotionDto(Promotion p) {
        return PromotionDto.builder()
                .id(p.getId())
                .shopId(p.getShop() != null ? p.getShop().getId() : null)
                .name(p.getName())
                .code(p.getCode())
                .promoType(p.getPromoType())
                .value(p.getDiscountValue())
                .minOrderAmount(p.getMinOrderValue())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .active(p.isActive())
                .build();
    }
}
