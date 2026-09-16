package com.storechain.group4.service.impl;

import com.storechain.group4.dto.CartItemDto;
import com.storechain.group4.dto.OrderCreateRequest;
import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ReviewDto;
import com.storechain.group4.model.CartItem;
import com.storechain.group4.model.Order;
import com.storechain.group4.model.OrderItem;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.ProductReview;
import com.storechain.group4.model.Promotion;
import com.storechain.group4.model.ReviewMedia;
import com.storechain.group4.model.ShippingAddress;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.Transporter;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.CartItemRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.ReviewMediaRepository;
import com.storechain.group4.repository.ShippingAddressRepository;
import com.storechain.group4.repository.TransporterRepository;
import com.storechain.group4.repository.UserRepository;
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

    // ============ Chi tiết sản phẩm ============

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductDetail(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        ProductResponseDto dto = toProductDto(p);
        dto.setSizesList(parseSizes(p.getSizes()));
        dto.setAvgRating(round1(reviewRepository.averageRatingByProductId(productId)));
        dto.setReviewCount(reviewRepository.countByProductId(productId));
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
    public CartItemDto addToCart(Long userId, Long productId, String size, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!"active".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("Sản phẩm này hiện không còn bán.");
        }
        if (product.getStock() <= 0) {
            throw new IllegalArgumentException("Sản phẩm đã hết hàng.");
        }
        int qty = Math.max(1, quantity);
        if (qty > product.getStock()) {
            throw new IllegalArgumentException("Chỉ còn " + product.getStock() + " sản phẩm trong kho.");
        }
        List<String> sizes = parseSizes(product.getSizes());
        String chosen = size == null ? "" : size.trim();
        if (!sizes.isEmpty() && !sizes.contains(chosen)) {
            throw new IllegalArgumentException("Vui lòng chọn size: " + String.join(", ", sizes));
        }
        if (sizes.isEmpty()) {
            chosen = "";
        }
        // Trùng sản phẩm + size thì cộng dồn số lượng
        CartItem item = cartItemRepository
                .findByUserIdAndProductIdAndSize(userId, productId, chosen)
                .orElse(null);
        if (item == null) {
            item = cartItemRepository.save(CartItem.builder()
                    .user(User.builder().id(userId).build())
                    .product(product)
                    .size(chosen)
                    .quantity(qty)
                    .build());
        } else {
            int newQty = item.getQuantity() + qty;
            if (newQty > product.getStock()) {
                throw new IllegalArgumentException("Tổng số lượng trong giỏ vượt quá tồn kho (" + product.getStock() + ").");
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
        if (quantity > item.getProduct().getStock()) {
            throw new IllegalArgumentException("Vượt quá tồn kho (" + item.getProduct().getStock() + ").");
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

    // ============ Đặt hàng từ giỏ ============

    @Override
    @Transactional
    public List<OrderDto> checkout(Long userId, OrderCreateRequest request) {
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

        // Gom sản phẩm theo chi nhánh -> mỗi chi nhánh một đơn
        Map<Long, List<CartItem>> byShop = cartItems.stream()
                .collect(Collectors.groupingBy(ci -> ci.getProduct().getShop().getId()));

        List<OrderDto> created = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : byShop.entrySet()) {
            created.add(createOrderForShop(userId, entry.getValue(), address, transporter, request));
        }
        cartItemRepository.deleteByUserId(userId);
        return created;
    }

    private OrderDto createOrderForShop(Long userId, List<CartItem> items,
                                        ShippingAddress address, Transporter transporter,
                                        OrderCreateRequest request) {
        Shop shop = items.get(0).getProduct().getShop();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : items) {
            totalAmount = totalAmount.add(ci.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        BigDecimal shippingFee = transporter != null && transporter.getBaseFee() != null
                ? transporter.getBaseFee()
                : BigDecimal.ZERO;

        // Áp khuyến mãi của chi nhánh (nếu nhập mã hợp lệ)
        BigDecimal discount = BigDecimal.ZERO;
        Promotion promo = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            promo = promotionRepository
                    .findByShopIdAndCodeIgnoreCaseAndActiveTrue(shop.getId(), request.getPromoCode().trim())
                    .orElse(null);
            if (promo == null) {
                throw new IllegalArgumentException("Mã khuyến mãi \"" + request.getPromoCode()
                        + "\" không tồn tại hoặc đã hết hiệu lực.");
            }
            LocalDateTime now = LocalDateTime.now();
            if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
                throw new IllegalArgumentException("Khuyến mãi \"" + promo.getName() + "\" đã hết hạn.");
            }
            if (promo.getMinOrderAmount() != null && totalAmount.compareTo(promo.getMinOrderAmount()) < 0) {
                throw new IllegalArgumentException("Đơn tối thiểu " + promo.getMinOrderAmount()
                        + "₫ để áp dụng khuyến mãi \"" + promo.getName() + "\".");
            }
            if (Promotion.TYPE_PERCENT.equals(promo.getPromoType())) {
                discount = totalAmount.multiply(promo.getValue())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (discount.compareTo(totalAmount) > 0) discount = totalAmount;
            } else {
                discount = promo.getValue().min(shippingFee);
            }
        }

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
                .promoName(promo != null ? promo.getName() : null)
                .transporter(transporter)
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
                    .build());
            // Trừ tồn kho
            p.setStock(Math.max(0, p.getStock() - ci.getQuantity()));
            productRepository.save(p);
        }
        return toOrderDto(order);
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

    // ============ Đơn hàng của tôi ============

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toOrderDto).toList();
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
        List<String> sizesList = parseSizes(p.getSizes());
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
                .sizesList(sizesList)
                .representative(p.isRepresentative())
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
                .shopId(p.getShop().getId())
                .shopName(p.getShop().getName())
                .price(p.getPrice())
                .size(ci.getSize())
                .quantity(ci.getQuantity())
                .stock(p.getStock())
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
}
