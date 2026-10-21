package com.storechain.group4.config;

import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.Favorite;
import com.storechain.group4.model.Order;
import com.storechain.group4.model.OrderItem;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.ProductReview;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.ShopCategory;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.FavoriteRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.OrderRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.ShopCategoryRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu demo để kiểm tra sắp xếp / top 20 / phân trang: 30 sản phẩm + 8 khách hàng mẫu +
 * đơn đã giao (quyết định "bán chạy"), đánh giá (quyết định "đánh giá cao"), lượt thích.
 *
 * Chỉ chạy khi app.seed-demo=true và chỉ chạy một lần (bỏ qua nếu đã có sản phẩm slug demo-sp-*).
 * Các thông số được rải lệch nhau theo chu kỳ khác nhau để mỗi kiểu sắp xếp cho ra thứ tự khác nhau.
 */
@Component
@org.springframework.core.annotation.Order(100) // sau DataInitializer (tạo role, shop, danh mục)
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private static final int PRODUCT_COUNT = 30;
    private static final int USER_COUNT = 8;
    private static final String SLUG_PREFIX = "demo-sp-";

    private static final String[] NAMES = {
            "Áo Thun Basic Cotton", "Áo Sơ Mi Oxford Tay Dài", "Quần Jean Slim Fit", "Áo Khoác Gió Nhẹ",
            "Quần Short Kaki", "Áo Polo Pique", "Áo Hoodie Nỉ Bông", "Quần Jogger Thể Thao",
            "Áo Len Cổ Tròn", "Váy Midi Xếp Ly", "Áo Sơ Mi Linen", "Quần Tây Công Sở",
            "Áo Thun Oversize In Hình", "Áo Khoác Denim", "Chân Váy Chữ A", "Áo Ba Lỗ Thể Thao",
            "Quần Culottes Ống Rộng", "Áo Cardigan Mỏng", "Áo Blazer Hàn Quốc", "Đầm Suông Cổ V",
            "Mũ Lưỡi Trai Thêu Logo", "Thắt Lưng Da Bò", "Ví Da Nam Gập Đôi", "Balo Vải Canvas",
            "Khăn Choàng Len", "Kính Râm Gọng Tròn", "Tất Cotton Cổ Cao (Set 5)", "Túi Tote Vải Bố",
            "Đồng Hồ Dây Da Mặt Tròn", "Cà Vạt Lụa Họa Tiết"
    };
    private static final String[] COMMENTS = {
            "Chất vải rất mát, đường may chắc chắn, mặc lên form đẹp đúng như mô tả. Sẽ ủng hộ shop thêm lần nữa.",
            "Giao hàng nhanh, đóng gói cẩn thận. Sản phẩm dùng được vài tuần thấy vẫn bền, màu không phai.",
            "Mình khá ưng, giá hợp lý so với chất lượng. Nhân viên tư vấn size chuẩn nên mặc vừa vặn luôn.",
            "Sản phẩm tạm ổn, có vài chỉ thừa nhưng không ảnh hưởng nhiều. Mong shop kiểm tra kỹ hơn trước khi gửi.",
            "Không đúng kỳ vọng lắm, màu hơi khác so với hình trên web và chất liệu mỏng hơn mình nghĩ."
    };

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ShopRepository shopRepository;
    private final ShopCategoryRepository shopCategoryRepository;
    private final ProductRepository productRepository;
    private final BranchStockRepository branchStockRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seed());
    }

    private void seed() {
        boolean already = productRepository.findAll().stream()
                .anyMatch(p -> p.getSlug() != null && p.getSlug().startsWith(SLUG_PREFIX));
        if (already) {
            log.info("Dữ liệu demo đã tồn tại, bỏ qua seed.");
            return;
        }
        Shop shop = shopRepository.findAll().stream().findFirst().orElse(null);
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER).orElse(null);
        if (shop == null || userRole == null) {
            log.warn("Chưa có chi nhánh hoặc role USER, bỏ qua seed demo.");
            return;
        }
        List<ShopCategory> cats = shopCategoryRepository.findByShopIdOrderBySortOrderAscIdAsc(shop.getId());

        List<User> users = createUsers(userRole);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < PRODUCT_COUNT; i++) {
            // 20 sản phẩm đầu thuộc danh mục 1 (quần áo), 10 sản phẩm sau thuộc danh mục 2 (phụ kiện)
            ShopCategory cat = cats.isEmpty() ? null : cats.get(i < 20 || cats.size() < 2 ? 0 : 1);
            // Giá lệch theo chu kỳ 20: 150.000₫ -> 1.100.000₫
            BigDecimal price = BigDecimal.valueOf(150_000L + ((i * 37L) % 20) * 50_000L);
            Product product = productRepository.save(Product.builder()
                    .name(NAMES[i])
                    .slug(String.format("%s%02d", SLUG_PREFIX, i + 1))
                    .description("Sản phẩm demo số " + (i + 1) + " dùng để kiểm tra sắp xếp và phân trang: " + NAMES[i] + ".")
                    .price(price)
                    .imageUrl("/img/product/product-" + (i % 14 + 1) + ".jpg")
                    .shop(shop)
                    .category(cat)
                    .stock(100)
                    .status("active")
                    .createdAt(now.minusHours(i * 6L)) // i càng nhỏ càng mới
                    .build());
            branchStockRepository.save(BranchStock.builder().product(product).shop(shop).stock(100).build());

            seedOrdersAndReviews(i, product, shop, users, price, now);
            seedFavorites(i, product, users);
        }
        log.info("Đã seed {} sản phẩm demo, {} khách hàng demo (mật khẩu: demo123).", PRODUCT_COUNT, USER_COUNT);
    }

    private List<User> createUsers(Role userRole) {
        List<User> users = new ArrayList<>();
        for (int j = 0; j < USER_COUNT; j++) {
            String username = "demo_user" + (j + 1);
            User u = userRepository.findByUsername(username).orElseGet(() -> userRepository.save(User.builder()
                    .username(username)
                    .email(username + "@demo.local")
                    .password(passwordEncoder.encode("demo123"))
                    .fullName("Khách Demo " + username.substring(username.length() - 1))
                    .phone("09000000" + String.format("%02d", users.size() + 1))
                    .status("active")
                    .role(userRole)
                    .build()));
            users.add(u);
        }
        return users;
    }

    /**
     * Sản phẩm i có (i*7)%6 đơn đã giao (0..5), mỗi đơn mua 1..3 cái, người mua cũng để lại đánh giá.
     * Cứ 9 sản phẩm có thêm một đơn bị hủy số lượng lớn để chứng minh đơn hủy không tính vào "bán chạy".
     */
    private void seedOrdersAndReviews(int i, Product product, Shop shop, List<User> users,
                                      BigDecimal price, LocalDateTime now) {
        int orderCount = (i * 7) % 6;
        for (int j = 0; j < orderCount; j++) {
            int qty = 1 + ((i + j) % 3);
            saveOrder(String.format("DEMO-%02d-%d", i + 1, j + 1), Order.STATUS_DELIVERED,
                    users.get(j), shop, product, price, qty, now.minusDays(2 + j));
            int rating = ((i * 3 + j * 2) % 5) + 1;
            reviewRepository.save(ProductReview.builder()
                    .product(product)
                    .user(users.get(j))
                    .rating(rating)
                    .comment(COMMENTS[rating - 1] + " (demo)")
                    .build());
        }
        if (i % 9 == 0) {
            saveOrder(String.format("DEMO-%02d-X", i + 1), Order.STATUS_CANCELLED,
                    users.get(USER_COUNT - 1), shop, product, price, 20, now.minusDays(1));
        }
    }

    private void saveOrder(String code, String status, User user, Shop shop, Product product,
                           BigDecimal price, int qty, LocalDateTime createdAt) {
        BigDecimal total = price.multiply(BigDecimal.valueOf(qty));
        Order order = orderRepository.save(Order.builder()
                .orderCode(code)
                .user(user)
                .shop(shop)
                .status(status)
                .totalAmount(total)
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .grandTotal(total)
                .paymentMethod(Order.PAYMENT_COD)
                .paymentStatus(Order.PAY_STATUS_UNPAID)
                .receiverName(user.getFullName())
                .receiverPhone("0900000000")
                .receiverAddress("123 Đường Demo, TP. Hồ Chí Minh")
                .cancelReason(Order.STATUS_CANCELLED.equals(status) ? "Đơn demo bị hủy" : null)
                .createdAt(createdAt)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .price(price)
                .quantity(qty)
                .build());
    }

    /** Sản phẩm i có (i*5)%7 lượt thích (0..6), chọn các khách khác nhau theo i */
    private void seedFavorites(int i, Product product, List<User> users) {
        int favCount = (i * 5) % 7;
        for (int k = 0; k < favCount; k++) {
            favoriteRepository.save(Favorite.builder()
                    .user(users.get((i + k) % USER_COUNT))
                    .product(product)
                    .build());
        }
    }
}
