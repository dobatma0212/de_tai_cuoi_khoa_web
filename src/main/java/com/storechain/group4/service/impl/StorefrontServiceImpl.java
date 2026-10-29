package com.storechain.group4.service.impl;

import com.storechain.group4.dto.CartItemDto;
import com.storechain.group4.dto.OrderCreateRequest;
import com.storechain.group4.dto.OrderDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ReviewDto;
import com.storechain.group4.model.BranchStock;
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
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.CartItemRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.ReviewMediaRepository;
import com.storechain.group4.repository.ShippingAddressRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.repository.TransporterRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.StorefrontService;
import com.storechain.group4.util.SizeStockUtil;
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

    /** Số ký tự tối thiểu của bình luận đánh giá */
    private static final int MIN_REVIEW_COMMENT_LENGTH = 50;

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
    private final com.storechain.group4.service.support.VietnamGeoService vietnamGeoService;
    private final com.storechain.group4.service.support.GeocodingService geocodingService;
    private final com.storechain.group4.service.PaymentService paymentService;
    private final com.storechain.group4.service.PromotionCalculator promotionCalculator;
    private final com.storechain.group4.repository.PromotionUsageRepository promotionUsageRepository;

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
        // Tồn kho toàn hãng = tổng tồn kho của tất cả chi nhánh
        int totalStock = branchStockRepository.sumStockByProductId(productId);
        dto.setStock(totalStock);
        // Tồn theo size tổng hợp toàn hệ thống (kho cũ chưa phân bổ size thì chia đều)
        java.util.Map<String, Integer> agg = new java.util.LinkedHashMap<>();
        for (BranchStock b : branchStockRepository.findByProductId(productId)) {
            int stock = Math.max(0, b.getStock());
            if (stock <= 0) continue;
            java.util.Map<String, Integer> part =
                    (b.getSizeStocks() == null || b.getSizeStocks().isBlank())
                            ? SizeStockUtil.splitEvenly(parseSizes(p.getSizes()), stock)
                            : SizeStockUtil.parseSizeStocks(b.getSizeStocks(), p.getSizes(), stock);
            part.forEach((k, v) -> agg.merge(k, Math.max(0, v), Integer::sum));
        }
        if (!agg.isEmpty()) dto.setTotalSizeStocks(agg);
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
        String trimmed = comment == null ? "" : comment.trim();
        if (trimmed.length() < MIN_REVIEW_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Bình luận phải có ít nhất " + MIN_REVIEW_COMMENT_LENGTH
                    + " ký tự (hiện có " + trimmed.length() + ").");
        }
        // Chỉ người đã mua và đã nhận sản phẩm (đơn Đã giao / Đã nhận hàng) mới được đánh giá
        if (orderItemRepository.countPurchasedByUser(userId, productId) == 0) {
            throw new IllegalArgumentException("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng.");
        }
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này rồi.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        ProductReview review = reviewRepository.save(ProductReview.builder()
                .product(product)
                .user(user)
                .rating(rating)
                .comment(trimmed)
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
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());
        return cartItemRepository.findByUserIdOrderByAddedAtDesc(userId)
                .stream().map(ci -> toCartDto(ci, activeFlashSales)).toList();
    }

    private void unusedGetCartRef() {
        cartItemRepository.findByUserIdOrderByAddedAtDesc(0L)
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
        List<String> sizes = parseSizes(product.getSizes());
        String chosen = size == null ? "" : size.trim();
        if (!sizes.isEmpty() && !sizes.contains(chosen)) {
            throw new IllegalArgumentException("Vui lòng chọn size: " + String.join(", ", sizes));
        }
        if (sizes.isEmpty()) {
            chosen = "";
        }
        // Kiểm tra số lượng theo ĐÚNG SIZE còn hàng trên toàn hệ thống
        int sizeStock = totalAvailableForSize(product, chosen);
        if (sizeStock <= 0) {
            throw new IllegalArgumentException("Size " + chosen + " của sản phẩm này đã hết hàng.");
        }
        if (qty > sizeStock) {
            throw new IllegalArgumentException("Size " + chosen + " chỉ còn " + sizeStock
                    + " sản phẩm trong kho.");
        }
        List<String> colors = parseSizes(product.getColor());
        String chosenColor = color == null ? "" : color.trim();
        if (!colors.isEmpty() && !colors.contains(chosenColor)) {
            throw new IllegalArgumentException("Vui lòng chọn màu: " + String.join(", ", colors));
        }
        if (colors.isEmpty()) {
            chosenColor = "";
        }
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
            int mergedStock = totalAvailableForSize(item.getProduct(), item.getSize());
            if (newQty > mergedStock) {
                throw new IllegalArgumentException("Tổng số lượng trong giỏ vượt quá tồn kho ("
                        + (item.getSize() == null || item.getSize().isBlank()
                                ? "còn " + mergedStock
                                : "size " + item.getSize() + " còn " + mergedStock) + ").");
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
        int sizeStock = totalAvailableForSize(item.getProduct(), item.getSize());
        if (quantity > sizeStock) {
            throw new IllegalArgumentException("Vượt quá tồn kho ("
                    + (item.getSize() == null || item.getSize().isBlank()
                            ? "còn " + sizeStock
                            : "size " + item.getSize() + " còn " + sizeStock) + ").");
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

    @Override
    @Transactional
    public List<OrderDto> checkout(Long userId, OrderCreateRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByAddedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống.");
        }
        // Kiểm tra số lượng từng dòng theo ĐÚNG SIZE trước khi tách đơn —
        // tránh tạo dở dang khi một dòng vượt tồn kho
        for (CartItem ci : cartItems) {
            int sizeStock = totalAvailableForSize(ci.getProduct(), ci.getSize());
            if (ci.getQuantity() > sizeStock) {
                throw new IllegalArgumentException("Sản phẩm \"" + ci.getProduct().getName()
                        + (ci.getSize() == null || ci.getSize().isBlank() ? "" : " size " + ci.getSize())
                        + "\" chỉ còn " + sizeStock + " sản phẩm. Vui lòng giảm số lượng trong giỏ.");
            }
        }
        ShippingAddress address = shippingAddressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có địa chỉ nhận hàng. Hãy thêm địa chỉ ở trang Hồ sơ cá nhân."));
        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        paymentService.assertConfigured(paymentMethod);
        // Các đơn tách từ một lần đặt dùng chung một mã thanh toán online
        String paymentRef = paymentService.isOnline(paymentMethod) ? paymentService.generatePaymentRef() : null;
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());
        BigDecimal cartTotal = cartItems.stream()
                .map(ci -> promotionCalculator.resolveEffectivePrice(activeFlashSales, ci.getProduct()).multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Voucher toàn hệ thống chỉ áp một lần cho đơn đầu tiên khi giỏ bị tách nhiều chi nhánh
        boolean[] globalVoucherUsed = {false};
        Transporter transporter = null;
        if (request.getTransporterId() != null && request.getTransporterId() > 0) {
            transporter = transporterRepository.findById(request.getTransporterId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà vận chuyển."));
        }

        // Với từng sản phẩm trong giỏ, chọn chi nhánh gần địa chỉ nhận hàng nhất CÒN HÀNG,
        // rồi gom các sản phẩm theo chi nhánh được phân phối -> mỗi chi nhánh một đơn.
        Map<Long, List<CartItem>> byShop = new java.util.LinkedHashMap<>();
        Map<Long, Shop> assignedShops = new java.util.HashMap<>();
        for (CartItem ci : cartItems) {
            Shop shop = pickNearestShopWithStock(ci.getProduct(), ci.getSize(), ci.getQuantity(), address);
            byShop.computeIfAbsent(shop.getId(), k -> new ArrayList<>()).add(ci);
            assignedShops.putIfAbsent(shop.getId(), shop);
        }

        List<OrderDto> created = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : byShop.entrySet()) {
            created.add(createOrderForShop(userId, assignedShops.get(entry.getKey()),
                    entry.getValue(), address, transporter, request,
                    paymentMethod, paymentRef, cartTotal, globalVoucherUsed, activeFlashSales));
        }
        cartItemRepository.deleteByUserId(userId);
        return created;
    }

    /**
     * Chọn chi nhánh phù hợp nhất để xử lý đơn của một sản phẩm (tính theo TỒN KHO SIZE):
     * 1. Chỉ xét chi nhánh active còn đủ hàng đúng size.
     * 2. Nhóm còn hàng, chọn chi nhánh GẦN KHÁCH NHẤT theo tọa độ thật từ
     *    Geocoding API (Photon/OpenStreetMap, có cache); API lỗi thì fallback
     *    về bảng tọa độ tâm tỉnh (VietnamGeoService). Khoảng cách nhỏ hơn thắng.
     * 3. Khi khoảng cách bằng nhau (cùng tỉnh) thì ưu tiên chi nhánh cùng quận/huyện,
     *    rồi chi nhánh có tồn kho size đó nhiều hơn.
     * Không xác định được tọa độ (địa chỉ lạ) thì rơi về xếp hạng so chuỗi tỉnh/quận cũ.
     */
    /**
     * Chọn chi nhánh phù hợp nhất để xử lý đơn của một sản phẩm (tính theo TỒN KHO SIZE):
     * 1. Ưu tiên tuyệt đối chi nhánh còn ĐỦ SỐ LƯỢNG khách đặt (đúng size).
     * 2. Trong nhóm đủ số lượng: khớp tỉnh + quận/huyện > chỉ khớp tỉnh > gần nhất theo tọa độ.
     * 3. Không chi nhánh nào đủ số lượng thì gán về chi nhánh có nhiều hàng nhất
     *    (cùng tỉnh/quận nếu có) để đảm bảo phần được giao nhiều nhất có thể.
     */
    private Shop pickNearestShopWithStock(Product product, String size, int quantity,
                                          ShippingAddress address) {
        List<Shop> shops = shopRepository.findByStatus("active");
        if (shops.isEmpty()) {
            throw new IllegalArgumentException(
                    "Hệ thống chưa có chi nhánh nào để xử lý đơn hàng của bạn.");
        }
        String province = normalizeAddressPart(address.getProvince());
        // Quận giữ nguyên tiền tố ("quan 1") — nếu bỏ prefix thì "Quận 1" thành "1"
        // và contains("1") khớp nhầm mọi địa chỉ có số 1 (VD số nhà của chi nhánh Quận 7)
        String district = normalizeKeepPrefix(address.getDistrict());
        // Tọa độ khách: geocode địa chỉ đầy đủ (đường + phường + quận + tỉnh);
        // thất bại thì rơi về tọa độ tâm tỉnh từ bảng nhúng sẵn
        String customerAddressStr = String.join(", ",
                java.util.stream.Stream.of(address.getDetailAddress(), address.getWard(),
                                address.getDistrict(), address.getProvince())
                        .filter(s -> s != null && !s.isBlank()).toList());
        double[] customerGeo = geocodingService.geocodeWithFallback(customerAddressStr,
                vietnamGeoService.resolve(address.getProvince()));
        if (customerGeo == null) customerGeo = vietnamGeoService.resolve(address.getProvince());

        BranchStock best = null;
        double bestDistanceKm = Double.MAX_VALUE;
        boolean bestEnough = false;
        int bestScore = -1;
        int bestStock = -1;
        for (Shop shop : shops) {
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(product.getId(), shop.getId()).orElse(null);
            int stock = availableForSize(bs, product, size);
            if (stock <= 0) continue;
            boolean enough = stock >= Math.max(1, quantity);

            int score = 0;
            String shopAddr = normalizeAddressPart(shop.getAddress());
            if (!province.isEmpty() && shopAddr.contains(province)) {
                score = 2;
                if (!district.isEmpty() && shopAddr.contains(district)) {
                    score = 3;
                }
            }
            // Tọa độ chi nhánh: geocode địa chỉ thật, lỗi thì fallback tâm tỉnh
            double[] shopGeo = geocodingService.geocodeWithFallback(shop.getAddress(),
                    vietnamGeoService.resolve(shop.getAddress()));
            if (shopGeo == null) shopGeo = vietnamGeoService.resolve(shop.getAddress());
            Double distanceKm = vietnamGeoService.distanceKm(customerGeo, shopGeo);

            // Thứ tự ưu tiên: (1) chi nhánh ĐỦ số lượng khách đặt;
            // (2) khớp tỉnh + quận/huyện > chỉ tỉnh > không khớp;
            // (3) trong cùng nhóm thì chọn GẦN hơn theo tọa độ thật;
            // (4) không đo được khoảng cách thì so tồn kho nhiều hơn.
            boolean better;
            if (enough != bestEnough) {
                better = enough;
            } else if (score != bestScore) {
                better = score > bestScore;
            } else if (distanceKm != null && bestDistanceKm != Double.MAX_VALUE) {
                better = distanceKm < bestDistanceKm;
            } else if (distanceKm != null) {
                better = true;
            } else if (bestDistanceKm == Double.MAX_VALUE) {
                better = stock > bestStock;
            } else {
                better = false;
            }
            if (better) {
                best = bs;
                bestEnough = enough;
                bestDistanceKm = distanceKm != null ? distanceKm : Double.MAX_VALUE;
                bestScore = score;
                bestStock = stock;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("Sản phẩm \"" + product.getName()
                    + (size == null || size.isBlank() ? "" : " size " + size)
                    + "\" hiện đã hết hàng tại tất cả chi nhánh.");
        }
        return best.getShop();
    }

    /**
     * Tổng số hàng có thể bán của một size trên TOÀN hệ thống (để kiểm tra số
     * lượng khách đặt vào giỏ / khi đặt hàng không vượt tồn kho theo size).
     */
    private int totalAvailableForSize(Product product, String size) {
        int total = 0;
        for (Shop shop : shopRepository.findByStatus("active")) {
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(product.getId(), shop.getId()).orElse(null);
            total += availableForSize(bs, product, size);
        }
        return total;
    }

    /**
     * Trừ tồn kho của một size, trả về chuỗi sizeStocks mới. Kho cũ chưa phân
     * bổ theo size được khởi tạo bằng cách chia đều (cùng quy ước với nhập hàng).
     * Trừ "mềm": nếu size không có trong phân bổ hoặc không đủ thì chỉ trừ về 0,
     * không chặn đặt hàng (tổng tồn đã được kiểm tra trước đó).
     */
    private String subtractSize(BranchStock bs, Product p, String size, int quantity) {
        if (quantity <= 0) return bs.getSizeStocks();
        Map<String, Integer> map = (bs.getSizeStocks() == null || bs.getSizeStocks().isBlank())
                ? SizeStockUtil.splitEvenly(SizeStockUtil.parseSizes(p.getSizes()), bs.getStock())
                : SizeStockUtil.parseSizeStocks(bs.getSizeStocks(), p.getSizes(), bs.getStock());
        String key = (size == null || size.isBlank())
                ? (map.containsKey("Tiêu chuẩn") ? "Tiêu chuẩn"
                        : map.keySet().stream().findFirst().orElse(null))
                : size;
        if (key == null || !map.containsKey(key)) return bs.getSizeStocks();
        map.put(key, Math.max(0, map.get(key) - quantity));
        return SizeStockUtil.toSizeStocksString(map);
    }

    /**
     * Số lượng còn có thể bán của một size tại chi nhánh. Sản phẩm không phân
     * size (hoặc chi nhánh chưa có phân bổ theo size — kho cũ) thì tính theo tổng tồn.
     */
    private int availableForSize(BranchStock bs, Product product, String size) {
        if (bs == null) return 0;
        int total = Math.max(0, bs.getStock());
        if (total <= 0) return 0;
        List<String> sizes = com.storechain.group4.util.SizeStockUtil.parseSizes(product.getSizes());
        if (sizes.isEmpty() || size == null || size.isBlank()) return total;
        if (bs.getSizeStocks() == null || bs.getSizeStocks().isBlank()) return total;
        var map = com.storechain.group4.util.SizeStockUtil
                .parseSizeStocks(bs.getSizeStocks(), product.getSizes(), total);
        return Math.max(0, map.getOrDefault(size, 0));
    }

    /**
     * Chuẩn hóa một phần địa chỉ để so khớp: bỏ tiền tố hành chính, bỏ dấu
     * tiếng Việt (địa chỉ khách nhập và địa chỉ chi nhánh admin lập có thể
     * khác nhau về dấu/kiểu viết — VD "Bình Dương" vs "Binh Duong").
     */
    private String normalizeAddressPart(String part) {
        if (part == null) return "";
        String p = part.trim().toLowerCase(java.util.Locale.ROOT);
        p = java.text.Normalizer.normalize(p, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        p = p.replace('đ', 'd');
        p = p.replaceAll("^(tinh|thanh pho|quan|huyen|q\\.|h\\.)\\s*", "");
        p = p.replaceAll("\\s+", " ");
        return p;
    }

    /** Như normalizeAddressPart nhưng GIỮ tiền tố hành chính (dùng cho so khớp quận/huyện) */
    private String normalizeKeepPrefix(String part) {
        if (part == null) return "";
        String p = part.trim().toLowerCase(java.util.Locale.ROOT);
        p = java.text.Normalizer.normalize(p, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        p = p.replace('đ', 'd');
        p = p.replaceAll("\\s+", " ");
        return p;
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return Order.PAYMENT_COD;
        }
        String m = method.trim().toUpperCase(java.util.Locale.ROOT);
        if (List.of(Order.PAYMENT_COD, Order.PAYMENT_VNPAY, Order.PAYMENT_MOMO).contains(m)) {
            return m;
        }
        throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ. Chọn COD, VNPAY hoặc MOMO.");
    }

    private OrderDto createOrderForShop(Long userId, Shop shop, List<CartItem> items,
                                        ShippingAddress address, Transporter transporter,
                                        OrderCreateRequest request,
                                        String paymentMethod, String paymentRef,
                                        BigDecimal cartTotal, boolean[] globalVoucherUsed,
                                        List<Promotion> activeFlashSales) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        java.util.Set<Long> usedFlashSaleIds = new java.util.HashSet<>();
        usedFlashSaleIds.clear();
        for (CartItem ci : items) {
            Promotion bestFs = promotionCalculator.findBestFlashSale(activeFlashSales, ci.getProduct().getId(), ci.getProduct().getPrice());
            if (bestFs != null && bestFs.getId() != null) {
                usedFlashSaleIds.add(bestFs.getId());
            }
            totalAmount = totalAmount.add(promotionCalculator.resolveEffectivePrice(activeFlashSales, ci.getProduct())
                    .multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        BigDecimal shippingFee = transporter != null && transporter.getBaseFee() != null
                ? transporter.getBaseFee()
                : BigDecimal.ZERO;

        // Áp khuyến mãi của chi nhánh (nếu nhập mã hợp lệ); mã của admin (toàn server) được xử lý ở PromotionService
        BigDecimal discount = BigDecimal.ZERO;
        Promotion promo = null;
        String promoName = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            String code = request.getPromoCode().trim();
            promo = promotionRepository
                    .findActiveByShopIdAndCodeIgnoreCase(shop.getId(), code)
                    .orElse(null);
            if (promo != null) {
                LocalDateTime now = LocalDateTime.now();
                if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
                    throw new IllegalArgumentException("Khuyến mãi \"" + promo.getName() + "\" đã hết hạn.");
                }
                if (promo.getMinOrderValue() != null && totalAmount.compareTo(promo.getMinOrderValue()) < 0) {
                    throw new IllegalArgumentException("Đơn tối thiểu " + promo.getMinOrderValue()
                            + "₫ để áp dụng khuyến mãi \"" + promo.getName() + "\".");
                }
                if (Promotion.TYPE_PERCENT.equals(promo.getPromoType())) {
                    discount = totalAmount.multiply(promo.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    if (discount.compareTo(totalAmount) > 0) discount = totalAmount;
                } else {
                    discount = promo.getDiscountValue().min(shippingFee);
                }
                promoName = promo.getName();
            } else {
                // Không phải mã của chi nhánh -> thử voucher toàn hệ thống (admin) mà khách chọn ở giỏ hàng
                Promotion global = promotionRepository.findByCodeIgnoreCaseAndShopIsNull(code).orElse(null);
                if (global == null) {
                    throw new IllegalArgumentException("Mã khuyến mãi \"" + request.getPromoCode()
                            + "\" không tồn tại hoặc đã hết hiệu lực.");
                }
                if (!globalVoucherUsed[0]) {
                    // Khóa dòng khuyến mãi để hai người cùng dùng mã cuối cùng không vượt giới hạn lượt dùng
                    global = promotionRepository.findByIdForUpdate(global.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Không thể áp dụng voucher, vui lòng thử lại."));
                    long usedByUser = promotionUsageRepository.countByPromotionIdAndUserId(global.getId(), userId);
                    String error = promotionCalculator.validateVoucher(global, cartTotal, usedByUser);
                    if (error != null) {
                        throw new IllegalArgumentException(error);
                    }
                    if (promotionRepository.incrementUsedCount(global.getId()) == 0) {
                        throw new IllegalArgumentException("Voucher đã hết lượt sử dụng.");
                    }
                    discount = promotionCalculator.calculateDiscount(global, cartTotal).min(totalAmount);
                    promotionUsageRepository.save(com.storechain.group4.model.PromotionUsage.builder()
                            .promotion(global)
                            .userId(userId)
                            .discountAmount(discount)
                            .orderTotal(cartTotal)
                            .build());
                    promoName = global.getName();
                    globalVoucherUsed[0] = true;
                }
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
                .promoName(promoName)
                .transporter(transporter)
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .receiverAddress(joinAddress(address))
                .note(request.getNote())
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentService.isOnline(paymentMethod)
                        ? Order.PAY_STATUS_PENDING : Order.PAY_STATUS_UNPAID)
                .paymentRef(paymentRef)
                .build());

        usedFlashSaleIds.clear();
        for (CartItem ci : items) {
            Promotion bestFs = promotionCalculator.findBestFlashSale(activeFlashSales, ci.getProduct().getId(), ci.getProduct().getPrice());
            if (bestFs != null && bestFs.getId() != null) {
                usedFlashSaleIds.add(bestFs.getId());
            }
            Product p = ci.getProduct();
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .product(p)
                    .productName(p.getName())
                    .imageUrl(p.getImageUrl())
                    .price(promotionCalculator.resolveEffectivePrice(activeFlashSales, p))
                    .quantity(ci.getQuantity())
                    .size(ci.getSize())
                    .color(ci.getColor())
                    .build());
            // Trừ tồn kho tại chi nhánh được phân phối (cả tổng và theo size)
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(p.getId(), shop.getId())
                    .orElse(BranchStock.builder().product(p).shop(shop).stock(0).build());
            bs.setStock(Math.max(0, bs.getStock() - ci.getQuantity()));
            bs.setSizeStocks(subtractSize(bs, p, ci.getSize(), ci.getQuantity()));
            for (Long fsId : usedFlashSaleIds) {
                promotionRepository.incrementUsedCount(fsId);
            }
            usedFlashSaleIds.clear();
            branchStockRepository.save(bs);
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
        // Hoàn tồn kho về chi nhánh phụ trách (cả tổng và theo size)
        for (var oi : orderItemRepository.findByOrderId(order.getId())) {
            if (oi.getProduct() == null) continue;
            BranchStock bs = branchStockRepository
                    .findByProductIdAndShopId(oi.getProduct().getId(), order.getShop().getId())
                    .orElse(BranchStock.builder().product(oi.getProduct())
                            .shop(order.getShop()).stock(0).build());
            bs.setStock(bs.getStock() + oi.getQuantity());
            bs.setSizeStocks(com.storechain.group4.util.SizeStockUtil.addToSize(
                    bs.getSizeStocks(), oi.getProduct().getSizes(), bs.getStock(),
                    oi.getSize(), oi.getQuantity()));
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
        List<String> sizesList = parseSizes(p.getSizes());
        List<Promotion> activeFlashSales = promotionRepository.findActiveFlashSales(LocalDateTime.now());
        ProductResponseDto dto = ProductResponseDto.builder()
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
                .color(p.getColor())
                .isNew(p.isNew())
                .isBestSeller(p.isBestSeller())
                .isFeatured(p.isFeatured())
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
        promotionCalculator.applyFlashSaleToDto(dto, p.getPrice(), activeFlashSales);
        if (true) return dto;
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
                .color(p.getColor())
                .isNew(p.isNew())
                .isBestSeller(p.isBestSeller())
                .isFeatured(p.isFeatured())
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
        return toCartDto(ci, promotionRepository.findActiveFlashSales(LocalDateTime.now()));
    }

    private CartItemDto toCartDto(CartItem ci, List<Promotion> activeFlashSales) {
        Product p = ci.getProduct();
        BigDecimal originalPrice = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
        Promotion best = promotionCalculator.findBestFlashSale(activeFlashSales, p.getId(), originalPrice);
        BigDecimal effectivePrice = best != null
                ? promotionCalculator.calculateFlashSalePrice(best, originalPrice)
                : originalPrice;
        Integer discountPercent = null;
        if (best != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            int pct = originalPrice.subtract(effectivePrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
                    .intValue();
            discountPercent = Math.max(1, pct);
        }
        return CartItemDto.builder()
                .id(ci.getId())
                .productId(p.getId())
                .productName(p.getName())
                .imageUrl(p.getImageUrl())
                .price(effectivePrice)
                .originalPrice(originalPrice)
                .onFlashSale(best != null)
                .flashSaleName(best != null ? best.getName() : null)
                .discountPercent(discountPercent)
                .size(ci.getSize())
                .color(ci.getColor())
                .quantity(ci.getQuantity())
                .stock(branchStockRepository.sumStockByProductId(p.getId()))
                .subtotal(effectivePrice.multiply(BigDecimal.valueOf(ci.getQuantity())))
                .build();
    }

    private CartItemDto unusedToCartDtoLegacy(CartItem ci) {
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
                .cancelReason(o.getCancelReason())
                .receiverName(o.getReceiverName())
                .receiverPhone(o.getReceiverPhone())
                .receiverAddress(o.getReceiverAddress())
                .note(o.getNote())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .paymentRef(o.getPaymentRef())
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
