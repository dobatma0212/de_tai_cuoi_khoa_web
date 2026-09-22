package com.storechain.group4.service.impl;

import com.storechain.group4.dto.CartItemDto;
import com.storechain.group4.dto.CheckoutResultDto;
import com.storechain.group4.dto.OrderCreateRequest;
import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.PromoPreviewDto;
import com.storechain.group4.dto.ReviewDto;
import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.CartItem;
import com.storechain.group4.model.Order;
import com.storechain.group4.model.OrderItem;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.ProductReview;
import com.storechain.group4.model.Promotion;
import com.storechain.group4.model.PromotionType;
import com.storechain.group4.model.PromotionUsage;
import com.storechain.group4.model.ReviewMedia;
import com.storechain.group4.model.ShippingAddress;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.Transporter;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.CartItemRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.PromotionUsageRepository;
import com.storechain.group4.repository.ReviewMediaRepository;
import com.storechain.group4.repository.ShippingAddressRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.repository.TransporterRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.PaymentService;
import com.storechain.group4.service.PromotionCalculator;
import com.storechain.group4.service.StorefrontService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorefrontServiceImpl implements StorefrontService {

    /** Số phút đơn đã xác nhận chuyển sang Đang giao (mặc định 2 phút) */
    @org.springframework.beans.factory.annotation.Value("${app.order.shipping-delay-minutes:2}")
    private long shippingDelayMinutes;

    /** Số phút đơn Đang giao chuyển sang Đã giao (mặc định 5 phút) */
    @org.springframework.beans.factory.annotation.Value("${app.order.deliver-delay-minutes:5}")
    private long deliverDelayMinutes;

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final TransporterRepository transporterRepository;
    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final BranchStockRepository branchStockRepository;
    private final ShopRepository shopRepository;
    private final PaymentService paymentService;
    private final PromotionCalculator promotionCalculator;
    private final PromotionUsageRepository promotionUsageRepository;

    // ============ Chi tiết sản phẩm ============

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductDetail(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        ProductResponseDto dto = toProductDto(p);
        dto.setAvgRating(round1(reviewRepository.averageRatingByProductId(productId)));
        dto.setReviewCount(reviewRepository.countByProductId(productId));
        // Tồn kho toàn hãng = tổng tồn kho của tất cả chi nhánh
        dto.setStock(branchStockRepository.sumStockByProductId(productId));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toReviewDto).toList();
    }

    @Override
    @Transactional
    public ReviewDto addReview(Long userId, Long productId, int rating, String comment,
                               List<ReviewDto.MediaDto> media) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        ProductReview review = reviewRepository.save(ProductReview.builder()
                .product(product)
                .user(user)
                .rating(rating)
                .comment(comment == null ? "" : comment.trim())
                .build());
        if (media != null) {
            for (ReviewDto.MediaDto m : media) {
                if (m.getUrl() == null || m.getUrl().isBlank()) continue;
                String type = "VIDEO".equalsIgnoreCase(m.getType()) ? "VIDEO" : "IMAGE";
                reviewMediaRepository.save(ReviewMedia.builder()
                        .review(review)
                        .url(m.getUrl().trim())
                        .mediaType(type)
                        .build());
            }
        }
        return toReviewDto(review);
    }

    // ============ Giỏ hàng ============

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDto> getCart(Long userId) {
        return cartItemRepository.findByUserIdOrderByAddedAtDesc(userId)
                .stream().map(this::toCartDto).toList();
    }

    @Override
    @Transactional
    public CartItemDto addToCart(Long userId, Long productId, String size, String color, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!"active".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("Sản phẩm này hiện không còn bán.");
        }
        int totalStock = branchStockRepository.sumStockByProductId(product.getId());
        if (totalStock <= 0) {
            throw new IllegalArgumentException("Sản phẩm đã hết hàng.");
        }
        int qty = Math.max(1, quantity);
        if (qty > totalStock) {
            throw new IllegalArgumentException("Chỉ còn " + totalStock + " sản phẩm trong kho.");
        }
        // Product mới không phân size/màu — chỉ lưu lựa chọn của khách nếu có gửi lên
        String chosen = size == null ? "" : size.trim();
        String chosenColor = color == null ? "" : color.trim();
        // Trùng sản phẩm + size + màu thì cộng dồn số lượng
        CartItem item = cartItemRepository
                .findByUserIdAndProductIdAndSizeAndColor(userId, productId, chosen, chosenColor)
                .orElse(null);
        if (item == null) {
            item = cartItemRepository.save(CartItem.builder()
                    .user(User.builder().id(userId).build())
                    .product(product)
                    .size(chosen)
                    .color(chosenColor)
                    .quantity(qty)
                    .build());
        } else {
            int newQty = item.getQuantity() + qty;
            if (newQty > branchStockRepository.sumStockByProductId(product.getId())) {
                throw new IllegalArgumentException("Tổng số lượng trong giỏ vượt quá tồn kho ("
                        + branchStockRepository.sumStockByProductId(product.getId()) + ").");
            }
            item.setQuantity(newQty);
            item = cartItemRepository.save(item);
        }
        return toCartDto(item);
    }

    @Override
    @Transactional
    public CartItemDto updateQuantity(Long userId, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng."));
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        }
        int totalStock = branchStockRepository.sumStockByProductId(item.getProduct().getId());
        if (quantity > totalStock) {
            throw new IllegalArgumentException("Vượt quá tồn kho (" + totalStock + ").");
        }
        item.setQuantity(quantity);
        return toCartDto(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long itemId) {
        cartItemRepository.findByIdAndUserId(itemId, userId)
                .ifPresent(cartItemRepository::delete);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCart(Long userId) {
        return cartItemRepository.countByUserId(userId);
    }

    // ============ Đặt hàng từ giỏ (phân phối về chi nhánh gần nhất) ============

    /** Một nhóm sản phẩm trong giỏ sẽ xử lý tại cùng một chi nhánh */
    private record ShopCart(Shop shop, List<CartItem> items) {}

    @Override
    @Transactional
    public CheckoutResultDto checkout(Long userId, OrderCreateRequest request, String clientIp) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByAddedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống.");
        }
        ShippingAddress address = shippingAddressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có địa chỉ nhận hàng. Hãy thêm địa chỉ ở trang Hồ sơ cá nhân."));
        Transporter transporter = null;
        if (request.getTransporterId() != null && request.getTransporterId() > 0) {
            transporter = transporterRepository.findById(request.getTransporterId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà vận chuyển."));
        }

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());

        // Với từng sản phẩm trong giỏ, chọn chi nhánh gần địa chỉ nhận hàng nhất CÒN HÀNG,
        // rồi gom các sản phẩm theo chi nhánh được phân phối -> mỗi chi nhánh một đơn.
        List<ShopCart> groups = groupCartByShop(cartItems, address);

        // Voucher là chương trình toàn cục: validate một lần trên tổng giỏ,
        // rồi phân bổ số tiền giảm về từng đơn chi nhánh theo tỷ lệ giá trị đơn.
        BigDecimal cartSubtotal = cartItems.stream()
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Promotion voucher = findValidVoucher(request.getPromoCode(), cartSubtotal, userId);
        BigDecimal totalDiscount = voucher == null ? BigDecimal.ZERO
                : promotionCalculator.calculateDiscount(voucher, cartSubtotal);
        Map<Long, BigDecimal> discountByShop = allocateDiscount(groups, cartSubtotal, totalDiscount);

        // Các đơn tách từ một lần đặt dùng chung mã giao dịch thanh toán
        String paymentRef = Order.PAYMENT_COD.equals(paymentMethod)
                ? null
                : "PAY-" + System.currentTimeMillis() + "-"
                        + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        List<OrderDto> created = new ArrayList<>();
        for (ShopCart group : groups) {
            created.add(createOrderForShop(userId, group, address, transporter,
                    request, paymentMethod, paymentRef,
                    discountByShop.getOrDefault(group.shop().getId(), BigDecimal.ZERO),
                    voucher != null && totalDiscount.signum() > 0 ? voucher.getName() : null));
        }
        cartItemRepository.deleteByUserId(userId);

        // Ghi nhận lượt dùng voucher sau khi mọi đơn đã tạo thành công
        if (voucher != null && totalDiscount.signum() > 0) {
            promotionRepository.incrementUsedCount(voucher.getId());
            promotionUsageRepository.save(PromotionUsage.builder()
                    .promotion(voucher)
                    .userId(userId)
                    .discountAmount(totalDiscount)
                    .orderTotal(cartSubtotal)
                    .build());
        }

        BigDecimal totalGrand = created.stream()
                .map(OrderDto::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String paymentUrl = null;
        boolean simulated = false;
        if (!Order.PAYMENT_COD.equals(paymentMethod)) {
            String orderCodes = created.stream()
                    .map(OrderDto::getOrderCode)
                    .collect(Collectors.joining(", "));
            PaymentService.PaymentUrl url = paymentService.createPaymentUrl(
                    paymentRef, totalGrand, paymentMethod,
                    "Thanh toan don hang " + orderCodes, clientIp);
            paymentUrl = url.url();
            simulated = url.simulated();
        }
        return CheckoutResultDto.builder()
                .orders(created)
                .paymentMethod(paymentMethod)
                .paymentRef(paymentRef)
                .totalGrandTotal(totalGrand)
                .paymentUrl(paymentUrl)
                .simulated(simulated)
                .build();
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank()) return Order.PAYMENT_COD;
        String m = method.trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of(Order.PAYMENT_COD, Order.PAYMENT_VNPAY, Order.PAYMENT_MOMO).contains(m)) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ (chỉ COD, VNPAY hoặc MOMO).");
        }
        return m;
    }

    private List<ShopCart> groupCartByShop(List<CartItem> cartItems, ShippingAddress address) {
        Map<Long, List<CartItem>> byShop = new java.util.LinkedHashMap<>();
        Map<Long, Shop> assignedShops = new java.util.HashMap<>();
        for (CartItem ci : cartItems) {
            Shop shop = pickNearestShopWithStock(ci.getProduct(), address);
            byShop.computeIfAbsent(shop.getId(), k -> new ArrayList<>()).add(ci);
            assignedShops.putIfAbsent(shop.getId(), shop);
        }
        return byShop.entrySet().stream()
                .map(e -> new ShopCart(assignedShops.get(e.getKey()), e.getValue()))
                .toList();
    }

    /**
     * Chọn chi nhánh phù hợp nhất để xử lý đơn của một sản phẩm:
     * 1. Ưu tiên chi nhánh active còn đủ hàng, cùng tỉnh + cùng quận/huyện với địa chỉ nhận.
     * 2. Sau đó đến chi nhánh cùng tỉnh còn hàng.
     * 3. Cuối cùng là bất kỳ chi nhánh active nào còn hàng (ưu tiên ít hàng trống trước).
     * Nếu không chi nhánh nào còn hàng thì ném lỗi.
     */
    private Shop pickNearestShopWithStock(Product product, ShippingAddress address) {
        List<Shop> shops = shopRepository.findAll().stream()
                .filter(s -> "active".equalsIgnoreCase(s.getStatus()))
                .toList();
        if (shops.isEmpty()) {
            throw new IllegalArgumentException(
                    "Hệ thống chưa có chi nhánh nào để xử lý đơn hàng của bạn.");
        }
        String province = normalizeAddressPart(address.getProvince());
        String district = normalizeAddressPart(address.getDistrict());

        BranchStock best = null;
        int bestScore = -1;
        int bestStock = -1;
        for (Shop shop : shops) {
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(product.getId(), shop.getId()).orElse(null);
            int stock = bs == null ? 0 : Math.max(0, bs.getStock());
            if (stock <= 0) continue;

            int score = 0;
            String shopAddr = normalizeAddressPart(shop.getAddress());
            if (!province.isEmpty() && shopAddr.contains(province)) {
                score = 2;
                if (!district.isEmpty() && shopAddr.contains(district)) {
                    score = 3;
                }
            }
            if (score > bestScore || (score == bestScore && stock > bestStock)) {
                best = bs;
                bestScore = score;
                bestStock = stock;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("Sản phẩm \"" + product.getName()
                    + "\" hiện đã hết hàng tại tất cả chi nhánh.");
        }
        return best.getShop();
    }

    /** Chuẩn hóa một phần địa chỉ để so khớp (bỏ tiền tố, thường hóa) */
    private String normalizeAddressPart(String part) {
        if (part == null) return "";
        String p = part.trim().toLowerCase(java.util.Locale.ROOT);
        p = p.replaceAll("^(tỉnh|tinh|thành phố|thanh pho|quận|quan|huyện|huyen|q\\.|h\\.)\\s*", "");
        p = p.replaceAll("\\s+", " ");
        return p;
    }

    private OrderDto createOrderForShop(Long userId, ShopCart group, ShippingAddress address,
                                        Transporter transporter, OrderCreateRequest request,
                                        String paymentMethod, String paymentRef,
                                        BigDecimal discount, String promoName) {
        Shop shop = group.shop();
        List<CartItem> items = group.items();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : items) {
            totalAmount = totalAmount.add(ci.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        BigDecimal shippingFee = transporter != null && transporter.getBaseFee() != null
                ? transporter.getBaseFee()
                : BigDecimal.ZERO;

        BigDecimal grandTotal = totalAmount.add(shippingFee).subtract(discount);

        Order order = orderRepository.save(Order.builder()
                .orderCode(generateOrderCode())
                .user(User.builder().id(userId).build())
                .shop(shop)
                .status(Order.STATUS_NEW)
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discountAmount(discount)
                .grandTotal(grandTotal)
                .promoName(promoName)
                .transporter(transporter)
                .paymentMethod(paymentMethod)
                .paymentStatus(Order.PAYMENT_COD.equals(paymentMethod)
                        ? Order.PAY_STATUS_UNPAID
                        : Order.PAY_STATUS_PENDING)
                .paymentRef(paymentRef)
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .receiverAddress(joinAddress(address))
                .note(request.getNote())
                .build());

        for (CartItem ci : items) {
            Product p = ci.getProduct();
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .product(p)
                    .productName(p.getName())
                    .imageUrl(p.getImageUrl())
                    .price(p.getPrice())
                    .quantity(ci.getQuantity())
                    .size(ci.getSize())
                    .color(ci.getColor())
                    .build());
            // Trừ tồn kho tại chi nhánh được phân phối
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(p.getId(), shop.getId())
                    .orElse(BranchStock.builder().product(p).shop(shop).stock(0).build());
            bs.setStock(Math.max(0, bs.getStock() - ci.getQuantity()));
            branchStockRepository.save(bs);
        }
        return toOrderDto(order);
    }

    /**
     * Tìm voucher hợp lệ theo mã (null nếu không nhập mã). Voucher thuộc hệ
     * khuyến mãi toàn cục — validate qua PromotionCalculator (trạng thái, thời gian,
     * lượt dùng, giá trị tối thiểu). Ném lỗi nếu mã không dùng được.
     */
    private Promotion findValidVoucher(String promoCode, BigDecimal orderTotal, Long userId) {
        if (promoCode == null || promoCode.isBlank()) {
            return null;
        }
        Promotion promo = promotionRepository.findByCodeIgnoreCase(promoCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá \"" + promoCode
                        + "\" không tồn tại."));
        if (promo.getPromotionType() != PromotionType.VOUCHER) {
            throw new IllegalArgumentException("Mã \"" + promoCode
                    + "\" không phải mã giảm giá dùng khi thanh toán.");
        }
        long userUsageCount = promotionUsageRepository
                .countByPromotionIdAndUserId(promo.getId(), userId);
        String error = promotionCalculator.validateVoucher(promo, orderTotal, userUsageCount);
        if (error != null) {
            throw new IllegalArgumentException("Mã \"" + promoCode + "\": " + error);
        }
        return promo;
    }

    /**
     * Phân bổ tổng số tiền giảm về từng đơn chi nhánh theo tỷ lệ giá trị đơn
     * (đơn cuối nhận phần lẻ để tổng khớp chính xác).
     */
    private Map<Long, BigDecimal> allocateDiscount(List<ShopCart> groups,
                                                   BigDecimal cartSubtotal,
                                                   BigDecimal totalDiscount) {
        Map<Long, BigDecimal> result = new java.util.LinkedHashMap<>();
        if (totalDiscount == null || totalDiscount.signum() <= 0
                || cartSubtotal.signum() <= 0) {
            groups.forEach(g -> result.put(g.shop().getId(), BigDecimal.ZERO));
            return result;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < groups.size(); i++) {
            ShopCart g = groups.get(i);
            BigDecimal groupTotal = g.items().stream()
                    .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal share;
            if (i == groups.size() - 1) {
                share = totalDiscount.subtract(allocated);
            } else {
                share = totalDiscount.multiply(groupTotal)
                        .divide(cartSubtotal, 0, RoundingMode.HALF_UP);
                allocated = allocated.add(share);
            }
            result.put(g.shop().getId(), share.max(BigDecimal.ZERO));
        }
        return result;
    }

    private String generateOrderCode() {
        String code = "ORD-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return orderRepository.existsByOrderCode(code) ? code + "-X" : code;
    }

    private String joinAddress(ShippingAddress a) {
        List<String> parts = Arrays.asList(
                a.getDetailAddress(), a.getWard(), a.getDistrict(), a.getProvince());
        return parts.stream().filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    // ============ Xem trước mã khuyến mãi ============

    @Override
    @Transactional(readOnly = true)
    public PromoPreviewDto previewPromo(Long userId, String promoCode, Long transporterId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByAddedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống.");
        }
        ShippingAddress address = shippingAddressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có địa chỉ nhận hàng. Hãy thêm địa chỉ ở trang Hồ sơ cá nhân."));
        Transporter transporter = null;
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (transporterId != null && transporterId > 0) {
            transporter = transporterRepository.findById(transporterId).orElse(null);
            if (transporter != null && transporter.getBaseFee() != null) {
                shippingFee = transporter.getBaseFee();
            }
        }

        List<ShopCart> groups = groupCartByShop(cartItems, address);
        BigDecimal subtotal = groups.stream()
                .flatMap(g -> g.items().stream())
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Voucher toàn cục: validate một lần trên tổng giỏ, phân bổ theo tỷ lệ đơn
        Promotion voucher = null;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        String promoError = null;
        if (promoCode != null && !promoCode.isBlank()) {
            try {
                voucher = findValidVoucher(promoCode, subtotal, userId);
                totalDiscount = promotionCalculator.calculateDiscount(voucher, subtotal);
            } catch (IllegalArgumentException e) {
                promoError = e.getMessage();
            }
        }
        Map<Long, BigDecimal> discountByShop = allocateDiscount(groups, subtotal, totalDiscount);

        List<PromoPreviewDto.ShopPreview> shops = new ArrayList<>();
        for (ShopCart group : groups) {
            BigDecimal total = group.items().stream()
                    .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discount = discountByShop.getOrDefault(group.shop().getId(), BigDecimal.ZERO);
            shops.add(PromoPreviewDto.ShopPreview.builder()
                    .shopId(group.shop().getId())
                    .shopName(group.shop().getName())
                    .totalAmount(total)
                    .discountAmount(discount)
                    .grandTotal(total.add(shippingFee).subtract(discount))
                    .promoName(voucher != null ? voucher.getName() : null)
                    .promoError(promoError)
                    .build());
        }

        BigDecimal grandTotal = subtotal.add(shippingFee).subtract(totalDiscount);
        return PromoPreviewDto.builder()
                .promoCode(promoCode == null || promoCode.isBlank() ? null : promoCode.trim())
                .promoName(voucher != null ? voucher.getName() : null)
                .valid(voucher != null && promoError == null)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(totalDiscount)
                .grandTotal(grandTotal)
                .shops(shops)
                .build();
    }

    // ============ Kết quả thanh toán từ cổng ============

    @Override
    @Transactional
    public int markPaymentResult(String paymentRef, boolean success) {
        List<Order> orders = orderRepository.findByPaymentRef(paymentRef);
        int updated = 0;
        for (Order order : orders) {
            if (!Order.PAY_STATUS_PENDING.equals(order.getPaymentStatus())) {
                continue;
            }
            if (success) {
                order.setPaymentStatus(Order.PAY_STATUS_PAID);
                orderRepository.save(order);
                updated++;
            } else {
                // Thanh toán thất bại: hủy đơn và hoàn tồn kho về chi nhánh
                order.setPaymentStatus(Order.PAY_STATUS_FAILED);
                order.setStatus(Order.STATUS_CANCELLED);
                order.setCancelReason("Thanh toán qua " + order.getPaymentMethod() + " thất bại");
                orderRepository.save(order);
                for (var oi : orderItemRepository.findByOrderId(order.getId())) {
                    if (oi.getProduct() == null) continue;
                    BranchStock bs = branchStockRepository
                            .findByProductIdAndShopId(oi.getProduct().getId(), order.getShop().getId())
                            .orElse(BranchStock.builder().product(oi.getProduct())
                                    .shop(order.getShop()).stock(0).build());
                    bs.setStock(bs.getStock() + oi.getQuantity());
                    branchStockRepository.save(bs);
                }
                updated++;
            }
        }
        return updated;
    }

    // ============ Đơn hàng của tôi ============

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getMyOrders(Long userId) {
        autoAdvanceStaleOrders();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toOrderDto).toList();
    }

    /** Đơn phải được xác nhận trước rồi mới tự trượt: Đã xác nhận qua X phút -> Đang giao -> Đã giao. Đơn NEW không bao giờ tự chuyển. */
    private void autoAdvanceStaleOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> toSave = new ArrayList<>();
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

    @Override
    @Transactional
    public OrderDto cancelMyOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Đây không phải đơn hàng của bạn.");
        }
        if (!List.of(Order.STATUS_NEW, Order.STATUS_CONFIRMED, Order.STATUS_SHIPPING,
                Order.STATUS_DELIVERED).contains(order.getStatus())) {
            throw new IllegalArgumentException(
                    "Đơn hàng đã được nhận/hoàn tất hoặc đang trong xử lý trả hàng, không thể hủy. Hãy dùng mục trả hàng.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn.");
        }
        order.setCancelReason(reason.trim() + " (khách hàng hủy)");
        order.setStatus(Order.STATUS_CANCELLED);
        // Hoàn tồn kho về chi nhánh phụ trách
        for (var oi : orderItemRepository.findByOrderId(order.getId())) {
            if (oi.getProduct() == null) continue;
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(oi.getProduct().getId(), order.getShop().getId())
                    .orElse(BranchStock.builder().product(oi.getProduct())
                            .shop(order.getShop()).stock(0).build());
            bs.setStock(bs.getStock() + oi.getQuantity());
            branchStockRepository.save(bs);
        }
        return toOrderDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto requestReturn(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Đây không phải đơn hàng của bạn.");
        }
        if (!List.of(Order.STATUS_DELIVERED, Order.STATUS_RECEIVED).contains(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ yêu cầu trả hàng khi đơn đã giao/đã nhận.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do trả hàng (VD: hàng lỗi, sai mẫu...).");
        }
        order.setCancelReason(reason.trim() + " (khách yêu cầu trả hàng - hoàn tiền)");
        order.setStatus(Order.STATUS_RETURN_REQUESTED);
        return toOrderDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto receiveMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Đây không phải đơn hàng của bạn.");
        }
        if (!Order.STATUS_DELIVERED.equals(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ xác nhận được khi đơn đã được giao tới bạn.");
        }
        order.setStatus(Order.STATUS_RECEIVED);
        // COD: khách nhận hàng đồng nghĩa đã thu tiền xong
        if (Order.PAYMENT_COD.equals(order.getPaymentMethod())
                && Order.PAY_STATUS_UNPAID.equals(order.getPaymentStatus())) {
            order.setPaymentStatus(Order.PAY_STATUS_PAID);
        }
        return toOrderDto(orderRepository.save(order));
    }

    // ============ Mapper ============

    private List<String> parseSizes(String sizes) {
        if (sizes == null || sizes.isBlank()) return List.of();
        return Arrays.stream(sizes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private Double round1(Double v) {
        if (v == null) return 0.0;
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
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
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ReviewDto toReviewDto(ProductReview r) {
        return ReviewDto.builder()
                .id(r.getId())
                .productId(r.getProduct().getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .userAvatar(r.getUser().getAvatarUrl())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .media(reviewMediaRepository.findByReviewId(r.getId())
                        .stream().map(m -> new ReviewDto.MediaDto(m.getUrl(), m.getMediaType()))
                        .collect(Collectors.toList()))
                .build();
    }

    private CartItemDto toCartDto(CartItem ci) {
        Product p = ci.getProduct();
        return CartItemDto.builder()
                .id(ci.getId())
                .productId(p.getId())
                .productName(p.getName())
                .imageUrl(p.getImageUrl())
                .price(p.getPrice())
                .size(ci.getSize())
                .color(ci.getColor())
                .quantity(ci.getQuantity())
                .stock(branchStockRepository.sumStockByProductId(p.getId()))
                .subtotal(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
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
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .paymentRef(o.getPaymentRef())
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
}
