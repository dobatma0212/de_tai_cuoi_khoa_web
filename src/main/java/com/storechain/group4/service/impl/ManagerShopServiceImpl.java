package com.storechain.group4.service.impl;

import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ProductPageDto;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.PromotionDto;
import com.storechain.group4.dto.RevenueDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopHomeDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.model.Order;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Promotion;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.ShopCategory;
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
import java.util.ArrayList;
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
    private final com.storechain.group4.repository.ProductReviewRepository reviewRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final String DEFAULT_CATEGORY_CLOTHING = "Bộ áo quần";
    private static final String DEFAULT_CATEGORY_ACCESSORY = "Phụ kiện";
    private static final int MAX_CATEGORIES_PER_SHOP = 20;

    // ============ Chi nhánh của manager ============

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getMyShop(Long managerId) {
        Shop shop = findMyShop(managerId);
        return toShopDto(shop);
    }

    @Override
    @Transactional
    public ShopResponseDto updateMyShop(Long managerId, ShopResponseDto request) {
        Shop shop = findMyShop(managerId);
        if (request.getName() != null && !request.getName().isBlank()) {
            shop.setName(request.getName().trim());
        }
        if (request.getAddress() != null) shop.setAddress(request.getAddress().trim());
        if (request.getPhone() != null) shop.setPhone(request.getPhone().trim());
        if (request.getDescription() != null) shop.setDescription(request.getDescription().trim());
        if (request.getLogoUrl() != null) shop.setLogoUrl(request.getLogoUrl().trim());
        if (request.getBannerUrl() != null) shop.setBannerUrl(request.getBannerUrl().trim());
        return toShopDto(shopRepository.save(shop));
    }

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

    @Override
    @Transactional
    public ShopCategoryDto addCategory(Long managerId, String name) {
        Shop shop = findMyShop(managerId);
        String trimmed = validateCategoryName(name);
        if (shopCategoryRepository.countByShopId(shop.getId()) >= MAX_CATEGORIES_PER_SHOP) {
            throw new IllegalArgumentException("Mỗi chi nhánh chỉ được tạo tối đa " + MAX_CATEGORIES_PER_SHOP + " danh mục.");
        }
        if (shopCategoryRepository.existsByShopIdAndNameIgnoreCase(shop.getId(), trimmed)) {
            throw new IllegalArgumentException("Danh mục \"" + trimmed + "\" đã tồn tại trong chi nhánh của bạn.");
        }
        ShopCategory saved = shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop)
                .name(trimmed)
                .sortOrder((int) shopCategoryRepository.countByShopId(shop.getId()))
                .build());
        return toCategoryDto(saved);
    }

    @Override
    @Transactional
    public ShopCategoryDto renameCategory(Long managerId, Long categoryId, String name) {
        Shop shop = findMyShop(managerId);
        ShopCategory category = findOwnedCategory(shop, categoryId);
        String trimmed = validateCategoryName(name);
        if (shopCategoryRepository.existsByShopIdAndNameIgnoreCaseAndIdNot(shop.getId(), trimmed, categoryId)) {
            throw new IllegalArgumentException("Danh mục \"" + trimmed + "\" đã tồn tại trong chi nhánh của bạn.");
        }
        category.setName(trimmed);
        return toCategoryDto(shopCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long managerId, Long categoryId) {
        Shop shop = findMyShop(managerId);
        ShopCategory category = findOwnedCategory(shop, categoryId);
        long productCount = productRepository.countByShopIdAndCategoryId(shop.getId(), categoryId);
        if (productCount > 0) {
            throw new IllegalArgumentException(
                    "Không thể xóa danh mục \"" + category.getName() + "\" vì còn " + productCount
                            + " sản phẩm. Hãy xóa/chuyển các sản phẩm trước.");
        }
        shopCategoryRepository.delete(category);
    }

    private String validateCategoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Tên danh mục tối đa 100 ký tự.");
        }
        return trimmed;
    }

    private ShopCategory findOwnedCategory(Shop shop, Long categoryId) {
        return shopCategoryRepository.findByIdAndShopId(categoryId, shop.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục trong chi nhánh của bạn."));
    }

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

    // ============ Sản phẩm chi nhánh ============

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

    @Override
    @Transactional
    public ProductResponseDto createProduct(Long managerId, ProductRequestDto request) {
        Shop shop = findMyShop(managerId);
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
                .build();
        Product saved = productRepository.save(product);
        // Sản phẩm đầu tiên của danh mục tự động làm đại diện
        if (!productRepository.existsByShopIdAndCategoryIdAndRepresentativeTrue(
                shop.getId(), category.getId())) {
            saved.setRepresentative(true);
            saved = productRepository.save(saved);
        }
        return toProductDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long managerId, Long productId, ProductRequestDto request) {
        Product product = findOwnedProduct(managerId, productId);
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
        }
        product.setSizes(normalizeSizes(request.getSizes()));
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
    public void deleteProduct(Long managerId, Long productId) {
        Product product = findOwnedProduct(managerId, productId);
        boolean wasRepresentative = product.isRepresentative();
        Long categoryId = product.getCategory().getId();
        Long shopId = product.getShop().getId();
        productRepository.delete(product);

        // Nếu xóa sản phẩm đại diện thì chọn sản phẩm khác thay thế
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
    public ProductResponseDto setRepresentative(Long managerId, Long productId, boolean representative) {
        Product product = findOwnedProduct(managerId, productId);
        if (representative) {
            // Bỏ cờ đại diện của các sản phẩm khác trong cùng danh mục
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

    private Product findOwnedProduct(Long managerId, Long productId) {
        Shop shop = findMyShop(managerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!product.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Sản phẩm này không thuộc chi nhánh bạn quản lý.");
        }
        return product;
    }

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

    // ============ Trang chủ chi nhánh (public) ============

    @Override
    @Transactional
    public ShopHomeDto getShopHome(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh."));
        ensureDefaultCategories(shop);
        List<ShopHomeDto.CategoryBlock> blocks = new ArrayList<>();
        for (ShopCategory category : shopCategoryRepository
                .findByShopIdOrderBySortOrderAscIdAsc(shop.getId())) {
            List<Product> products = productRepository
                    .findByShopIdAndCategoryIdOrderByCreatedAtDesc(shop.getId(), category.getId());
            Product rep = products.stream().filter(Product::isRepresentative).findFirst()
                    .orElse(products.stream().findFirst().orElse(null));
            blocks.add(ShopHomeDto.CategoryBlock.builder()
                    .categoryId(category.getId())
                    .label(category.getName())
                    .representativeProduct(rep == null ? null : toProductDto(rep))
                    .totalProducts(products.size())
                    .build());
        }
        return ShopHomeDto.builder()
                .shop(toShopDto(shop))
                .categories(blocks)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPageDto searchShopProducts(Long shopId, String keyword, Long categoryId,
                                             BigDecimal minPrice, BigDecimal maxPrice,
                                             String size, String sort, int page, int pageSize) {
        String kw = keyword == null ? "" : keyword.trim();
        List<Product> products;
        if (categoryId != null && categoryId > 0) {
            products = productRepository.findByShopIdAndCategoryIdOrderByCreatedAtDesc(shopId, categoryId);
        } else if (!kw.isEmpty()) {
            products = productRepository.searchProducts(shopId, kw);
        } else {
            products = productRepository.findByShopIdOrderByCreatedAtDesc(shopId);
        }

        final String kwLower = kw.toLowerCase(Locale.ROOT);
        List<Product> filtered = products.stream()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                .filter(p -> kw.isEmpty()
                        || p.getName().toLowerCase(Locale.ROOT).contains(kwLower)
                        || (p.getDescription() != null && p.getDescription()
                                .toLowerCase(Locale.ROOT).contains(kwLower)))
                .filter(p -> minPrice == null
                        || p.getPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null
                        || p.getPrice().compareTo(maxPrice) <= 0)
                .filter(p -> size == null || size.isBlank()
                        || sizeMatches(p.getSizes(), size.trim()))
                .collect(Collectors.toList());

        if ("priceAsc".equals(sort)) {
            filtered.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
        } else if ("priceDesc".equals(sort)) {
            filtered.sort((a, b) -> b.getPrice().compareTo(a.getPrice()));
        } else if ("name".equals(sort)) {
            filtered.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        } // mặc định: mới nhất (đã sort theo created_at từ repository)

        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) filtered.size() / safePageSize);
        int from = Math.min(safePage * safePageSize, filtered.size());
        int to = Math.min(from + safePageSize, filtered.size());

        List<ProductResponseDto> content = filtered.subList(from, to)
                .stream().map(this::toProductDto).collect(Collectors.toList());
        // Điểm đánh giá từng sản phẩm (chỉ 10 phần tử/trang nên chi phí thấp)
        for (ProductResponseDto dto : content) {
            dto.setAvgRating(round1(reviewRepository.averageRatingByProductId(dto.getId())));
            dto.setReviewCount(reviewRepository.countByProductId(dto.getId()));
        }

        return ProductPageDto.builder()
                .content(content)
                .page(safePage)
                .pageSize(safePageSize)
                .totalElements(filtered.size())
                .totalPages(totalPages)
                .build();
    }

    /** Kiểm tra size có nằm trong danh sách size của sản phẩm (VD: "M" trong "S,M,L") */
    private boolean sizeMatches(String sizes, String wanted) {
        if (sizes == null || sizes.isBlank()) return false;
        return Arrays.stream(sizes.split(","))
                .map(String::trim)
                .anyMatch(s -> s.equalsIgnoreCase(wanted));
    }

    /** "S,M,L" -> ["S","M","L"] */
    private List<String> parseSizes(String sizes) {
        if (sizes == null || sizes.isBlank()) return List.of();
        return Arrays.stream(sizes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Double round1(Double v) {
        if (v == null) return 0.0;
        return BigDecimal.valueOf(v).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> suggestShopProducts(Long shopId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return productRepository.suggestByName(shopId, keyword.trim())
                .stream().limit(8).map(this::toProductDto).toList();
    }

    // ============ Quản lý đơn hàng của chi nhánh ============

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrders(Long managerId, String status) {
        Shop shop = findMyShop(managerId);
        List<Order> orders = (status == null || status.isBlank())
                ? orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                : orderRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shop.getId(), status);
        return orders.stream().map(this::toOrderDto).toList();
    }

    @Override
    @Transactional
    public OrderDto updateOrderStatus(Long managerId, Long orderId, String status) {
        Shop shop = findMyShop(managerId);
        List<String> allowed = List.of(
                Order.STATUS_NEW, Order.STATUS_CONFIRMED, Order.STATUS_PICKED_UP,
                Order.STATUS_SHIPPING, Order.STATUS_DELIVERED,
                Order.STATUS_CANCELLED, Order.STATUS_RETURNED);
        if (!allowed.contains(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc chi nhánh bạn quản lý.");
        }
        order.setStatus(status);
        return toOrderDto(orderRepository.save(order));
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
        promo.setName(request.getName().trim());
        promo.setCode(request.getCode() == null || request.getCode().isBlank()
                ? null : request.getCode().trim());
        promo.setPromoType(request.getPromoType());
        promo.setValue(request.getValue());
        promo.setMinOrderAmount(request.getMinOrderAmount() == null
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

        for (Order o : orders) {
            if (Order.STATUS_DELIVERED.equals(o.getStatus())) {
                delivered++;
                revenue = revenue.add(o.getGrandTotal() == null ? BigDecimal.ZERO : o.getGrandTotal());
                productAmount = productAmount.add(o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount());
                shippingTotal = shippingTotal.add(o.getShippingFee() == null ? BigDecimal.ZERO : o.getShippingFee());
                discountTotal = discountTotal.add(o.getDiscountAmount() == null ? BigDecimal.ZERO : o.getDiscountAmount());
            } else if (Order.STATUS_CANCELLED.equals(o.getStatus())) {
                cancelled++;
            } else if (Order.STATUS_RETURNED.equals(o.getStatus())) {
                returned++;
            }
        }
        return RevenueDto.builder()
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
                .sizesList(parseSizes(p.getSizes()))
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
            case Order.STATUS_CANCELLED -> "Hủy";
            case Order.STATUS_RETURNED -> "Trả hàng - hoàn tiền";
            default -> status;
        };
    }

    private PromotionDto toPromotionDto(Promotion p) {
        return PromotionDto.builder()
                .id(p.getId())
                .shopId(p.getShop().getId())
                .name(p.getName())
                .code(p.getCode())
                .promoType(p.getPromoType())
                .value(p.getValue())
                .minOrderAmount(p.getMinOrderAmount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .active(p.isActive())
                .build();
    }
}
