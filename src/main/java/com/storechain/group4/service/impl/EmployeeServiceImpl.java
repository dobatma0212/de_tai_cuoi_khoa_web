package com.storechain.group4.service.impl;

import com.storechain.group4.dto.EmployeeStatsDto;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.EmployeeWorkday;
import com.storechain.group4.model.Order;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.ShopCategory;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.CartItemRepository;
import com.storechain.group4.repository.EmployeeWorkdayRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.ReviewMediaRepository;
import com.storechain.group4.repository.ShopCategoryRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.EmployeeService;
import com.storechain.group4.service.ManagerShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final ShopCategoryRepository shopCategoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchStockRepository branchStockRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final EmployeeWorkdayRepository workdayRepository;
    private final com.storechain.group4.repository.TransporterRepository transporterRepository;
    private final ManagerShopService managerShopService; // dùng chung logic upload ảnh

    @Value("${app.employee.commission-rate:0.02}")
    private BigDecimal commissionRate;

    /** Số phút đơn đã xác nhận chuyển sang Đang giao (mặc định 2 phút) */
    @Value("${app.order.shipping-delay-minutes:2}")
    private long shippingDelayMinutes;

    /** Số phút đơn Đang giao chuyển sang Đã giao (mặc định 5 phút) */
    @Value("${app.order.deliver-delay-minutes:5}")
    private long deliverDelayMinutes;

    private static final String DEFAULT_CATEGORY_CLOTHING = "Bộ áo quần";
    private static final String DEFAULT_CATEGORY_ACCESSORY = "Phụ kiện";

    // ============ Chi nhánh của nhân viên ============

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getMyShop(Long employeeId) {
        return toShopDto(findMyShop(employeeId));
    }

    /** Nhân viên làm việc ở chi nhánh được gán trong cột users.shop_id */
    private Shop findMyShop(Long employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhân viên."));
        Shop shop = user.getShop();
        if (shop == null) {
            throw new IllegalArgumentException(
                    "Bạn chưa được phân công vào chi nhánh nào. Vui lòng liên hệ quản trị viên.");
        }
        return shop;
    }

    @Override
    @Transactional
    public List<ShopCategoryDto> getMyCategories(Long employeeId) {
        Shop shop = findMyShop(employeeId);
        ensureDefaultCategories(shop);
        return shopCategoryRepository.findByShopIdOrderBySortOrderAscIdAsc(shop.getId())
                .stream().map(this::toCategoryDto).toList();
    }

    /** Chi nhánh chưa có danh mục thì tạo sẵn 2 danh mục mặc định (như manager) */
    private void ensureDefaultCategories(Shop shop) {
        if (shopCategoryRepository.countByShopId(shop.getId()) > 0) {
            return;
        }
        shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop).name(DEFAULT_CATEGORY_CLOTHING).sortOrder(0).build());
        shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop).name(DEFAULT_CATEGORY_ACCESSORY).sortOrder(1).build());
    }

    // ============ Sản phẩm của chi nhánh ============

    @Override
    @Transactional
    public List<ProductResponseDto> getMyProducts(Long employeeId, Long categoryId) {
        Shop shop = findMyShop(employeeId);
        ensureDefaultCategories(shop);
        List<Product> products = (categoryId == null || categoryId <= 0)
                ? productRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                : productRepository.findByShopIdAndCategoryIdOrderByCreatedAtDesc(shop.getId(), categoryId);
        return products.stream().map(this::toProductDto).toList();
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(Long employeeId, ProductRequestDto request) {
        Shop shop = findMyShop(employeeId);
        ensureDefaultCategories(shop);
        validatePrice(request);
        ShopCategory category = findOwnedCategory(shop, request.getCategoryId());
        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .name(request.getName().trim())
                .price(request.getPrice())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .stock(request.getStock() == null ? 0 : Math.max(0, request.getStock()))
                .sizes(normalizeSizes(request.getSizes()))
                .color(request.getColor() == null ? null : request.getColor().trim())
                .build();
        Product saved = productRepository.save(product);
        syncBranchStock(shop, saved, product.getStock());
        if (!productRepository.existsByShopIdAndCategoryIdAndRepresentativeTrue(
                shop.getId(), category.getId())) {
            saved.setRepresentative(true);
            saved = productRepository.save(saved);
        }
        return toProductDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long employeeId, Long productId, ProductRequestDto request) {
        Product product = findOwnedProduct(employeeId, productId);
        validatePrice(request);
        ShopCategory newCategory = findOwnedCategory(product.getShop(), request.getCategoryId());
        boolean categoryChanged = !product.getCategory().getId().equals(newCategory.getId());
        product.setCategory(newCategory);
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        if (request.getStock() != null) {
            product.setStock(Math.max(0, request.getStock()));
            syncBranchStock(product.getShop(), product, product.getStock());
        }
        product.setSizes(normalizeSizes(request.getSizes()));
        product.setColor(request.getColor() == null ? null : request.getColor().trim());
        if (categoryChanged) {
            product.setRepresentative(false);
            Product saved = productRepository.save(product);
            if (!productRepository.existsByShopIdAndCategoryIdAndRepresentativeTrue(
                    saved.getShop().getId(), newCategory.getId())) {
                saved.setRepresentative(true);
                saved = productRepository.save(saved);
            }
            return toProductDto(saved);
        }
        return toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long employeeId, Long productId) {
        Product product = findOwnedProduct(employeeId, productId);
        boolean wasRepresentative = product.isRepresentative();
        Long categoryId = product.getCategory().getId();
        Long shopId = product.getShop().getId();

        // Dọn dữ liệu liên quan (như manager): tồn kho, giỏ hàng, đánh giá, tham chiếu đơn hàng
        branchStockRepository.deleteAll(branchStockRepository.findByProductId(productId));
        cartItemRepository.deleteAll(cartItemRepository.findByProductId(productId));
        reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).forEach(review -> {
            reviewMediaRepository.deleteAll(reviewMediaRepository.findByReviewId(review.getId()));
            reviewRepository.delete(review);
        });
        orderItemRepository.findByProductId(productId).forEach(oi -> {
            oi.setProduct(null);
            orderItemRepository.save(oi);
        });

        productRepository.delete(product);

        if (wasRepresentative) {
            List<Product> remaining = productRepository
                    .findByShopIdAndCategoryIdOrderByCreatedAtDesc(shopId, categoryId);
            if (!remaining.isEmpty()) {
                Product next = remaining.get(0);
                next.setRepresentative(true);
                productRepository.save(next);
            }
        }
    }

    @Override
    @Transactional
    public ProductResponseDto setRepresentative(Long employeeId, Long productId, boolean representative) {
        Product product = findOwnedProduct(employeeId, productId);
        if (representative) {
            productRepository.findByShopIdAndCategoryIdAndRepresentativeTrue(
                            product.getShop().getId(), product.getCategory().getId())
                    .forEach(p -> {
                        if (!p.getId().equals(productId)) {
                            p.setRepresentative(false);
                            productRepository.save(p);
                        }
                    });
            product.setRepresentative(true);
        } else {
            product.setRepresentative(false);
        }
        return toProductDto(productRepository.save(product));
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return managerShopService.uploadImage(file);
    }

    private String normalizeSizes(String sizes) {
        if (sizes == null) return null;
        String cleaned = sizes.replaceAll("\\s*,\\s*", ",").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void validatePrice(ProductRequestDto request) {
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá sản phẩm phải là số VND hợp lệ (>= 0).");
        }
    }

    private void syncBranchStock(Shop shop, Product product, int stock) {
        BranchStock bs = branchStockRepository
                .findByProductIdAndShopId(product.getId(), shop.getId())
                .orElse(BranchStock.builder().product(product).shop(shop).stock(0).build());
        bs.setStock(Math.max(0, stock));
        branchStockRepository.save(bs);
    }

    private ShopCategory findOwnedCategory(Shop shop, Long categoryId) {
        return shopCategoryRepository.findByIdAndShopId(categoryId, shop.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy danh mục trong chi nhánh của bạn."));
    }

    private Product findOwnedProduct(Long employeeId, Long productId) {
        Shop shop = findMyShop(employeeId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!product.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Sản phẩm này không thuộc chi nhánh bạn đang làm việc.");
        }
        return product;
    }


    // ============ Đơn hàng của chi nhánh (như manager) ============

    @Override
    @Transactional(readOnly = true)
    public List<com.storechain.group4.dto.OrderDto> getMyOrders(Long employeeId, String status) {
        Shop shop = findMyShop(employeeId);
        autoAdvanceStaleOrders();
        List<Order> orders = (status == null || status.isBlank())
                ? orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                : orderRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shop.getId(), status);
        return orders.stream().map(this::toOrderDto).toList();
    }

    /**
     * Nhân viên chỉ được (giống manager):
     * - CONFIRMED: xác nhận đơn NEW (kèm đơn vị vận chuyển). Đơn được gắn handledBy
     *   là nhân viên xác nhận -> giao thành công sẽ tính hoa hồng cho nhân viên đó.
     * - CANCELLED: hủy đơn NEW/CONFIRMED/SHIPPING (bắt buộc lý do), hoàn tồn kho.
     * - RETURNED: đánh dấu trả hàng - hoàn tiền cho đơn đã giao/khách đã nhận.
     */
    @Override
    @Transactional
    public com.storechain.group4.dto.OrderDto updateOrderStatus(Long employeeId, Long orderId, String status,
                                                                Long transporterId, String reason) {
        Shop shop = findMyShop(employeeId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc chi nhánh bạn đang làm việc.");
        }
        String current = order.getStatus();
        switch (status == null ? "" : status) {
            case Order.STATUS_CONFIRMED -> {
                if (!Order.STATUS_NEW.equals(current)) {
                    throw new IllegalArgumentException("Chỉ đơn ở trạng thái mới mới có thể xác nhận.");
                }
                if (transporterId == null) {
                    throw new IllegalArgumentException("Vui lòng chọn đơn vị vận chuyển trước khi xác nhận.");
                }
                var transporter = transporterRepository.findById(transporterId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển."));
                order.setTransporter(transporter);
                if (order.getShippingFee() == null || order.getShippingFee().compareTo(BigDecimal.ZERO) <= 0) {
                    order.setShippingFee(transporter.getBaseFee() == null ? BigDecimal.ZERO : transporter.getBaseFee());
                    order.setGrandTotal(order.getTotalAmount()
                            .add(order.getShippingFee())
                            .subtract(order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount()));
                }
                order.setStatus(Order.STATUS_CONFIRMED);
                // Gắn nhân viên phụ trách đơn -> cơ sở tính hoa hồng khi đơn hoàn tất
                if (order.getHandledBy() == null) {
                    order.setHandledBy(userRepository.findById(employeeId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhân viên.")));
                }
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

    /** Hoàn lại tồn kho các sản phẩm của đơn bị hủy/trả về chi nhánh */
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

    /** Đẩy mốc giao hàng tự động như manager: xác nhận quá 10s -> đang giao -> đã giao */
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

    private com.storechain.group4.dto.OrderDto toOrderDto(Order o) {
        List<com.storechain.group4.dto.OrderDto.ItemDto> items = orderItemRepository.findByOrderId(o.getId())
                .stream().map(oi -> com.storechain.group4.dto.OrderDto.ItemDto.builder()
                        .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                        .productName(oi.getProductName())
                        .imageUrl(oi.getImageUrl())
                        .price(oi.getPrice())
                        .quantity(oi.getQuantity())
                        .size(oi.getSize())
                        .color(oi.getColor())
                        .subtotal(oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                        .build())
                .toList();
        return com.storechain.group4.dto.OrderDto.builder()
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

    // ============ Chấm công (tính công) ============

    @Override
    @Transactional
    public EmployeeStatsDto checkIn(Long employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhân viên."));
        LocalDate today = LocalDate.now();
        if (workdayRepository.findByUserIdAndWorkDate(employeeId, today).isPresent()) {
            throw new IllegalArgumentException("Hôm nay bạn đã chấm công rồi.");
        }
        workdayRepository.save(EmployeeWorkday.builder()
                .user(user)
                .workDate(today)
                .checkInAt(LocalDateTime.now())
                .build());
        return getMyStats(employeeId, null);
    }

    // ============ Hoa hồng & thống kê theo tháng ============

    @Override
    @Transactional(readOnly = true)
    public EmployeeStatsDto getMyStats(Long employeeId, String month) {
        YearMonth ym = parseMonth(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        long workDays = workdayRepository.countByUserIdAndWorkDateBetween(employeeId, start, end);
        List<LocalDate> workDates = workdayRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, start, end)
                .stream().map(EmployeeWorkday::getWorkDate).toList();

        // Sao kê: TẤT CẢ đơn do nhân viên phụ trách trong tháng (mọi trạng thái)
        List<OrderCommissionHelper> handled = orderRepository.findByHandledByIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .filter(o -> o.getCreatedAt() != null
                        && !o.getCreatedAt().toLocalDate().isBefore(start)
                        && !o.getCreatedAt().toLocalDate().isAfter(end))
                .map(o -> new OrderCommissionHelper(o.getId(), o.getOrderCode(), o.getStatus(),
                        o.getGrandTotal() == null ? BigDecimal.ZERO : o.getGrandTotal(),
                        o.getCreatedAt()))
                .toList();
        // Đơn "đã bán được": đã giao / khách đã nhận — đơn hủy hoặc trả không tính hoa hồng
        List<OrderCommissionHelper> sold = handled.stream()
                .filter(h -> Order.STATUS_DELIVERED.equals(h.status())
                        || Order.STATUS_RECEIVED.equals(h.status()))
                .toList();

        BigDecimal totalSales = sold.stream()
                .map(OrderCommissionHelper::grandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = totalSales.multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        return EmployeeStatsDto.builder()
                .month(ym.toString())
                .workDays(workDays)
                .ordersSold(sold.size())
                .totalSales(totalSales)
                .commissionRate(commissionRate)
                .commission(commission)
                .orders(sold.stream().map(this::toOrderCommission).toList())
                .handledOrders(handled.stream().map(this::toOrderCommission).toList())
                .workDates(workDates)
                .build();
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Tháng không hợp lệ, dùng định dạng yyyy-MM (VD: 2026-09).");
        }
    }

    private EmployeeStatsDto.OrderCommission toOrderCommission(OrderCommissionHelper h) {
        return EmployeeStatsDto.OrderCommission.builder()
                .orderId(h.orderId())
                .orderCode(h.orderCode())
                .status(h.status())
                .grandTotal(h.grandTotal())
                .createdAt(h.createdAt())
                .build();
    }

    /** Bản ghi nội bộ để tính tổng doanh số của nhân viên */
    private record OrderCommissionHelper(Long orderId, String orderCode, String status,
                                         BigDecimal grandTotal, LocalDateTime createdAt) {
    }

    // ============ Mapper ============

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

    private ShopCategoryDto toCategoryDto(ShopCategory c) {
        return ShopCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .sortOrder(c.getSortOrder())
                .totalProducts(productRepository.countByShopIdAndCategoryId(c.getShop().getId(), c.getId()))
                .build();
    }

    private ProductResponseDto toProductDto(Product p) {
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
                .sizes(p.getSizes())
                .sizesList(p.getSizes() == null || p.getSizes().isBlank() ? List.of()
                        : Arrays.stream(p.getSizes().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList())
                .color(p.getColor())
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
