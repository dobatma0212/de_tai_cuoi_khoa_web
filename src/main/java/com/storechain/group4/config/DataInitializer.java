package com.storechain.group4.config;

import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Transporter;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.TransporterRepository;
import com.storechain.group4.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final com.storechain.group4.repository.CategoryRepository categoryRepository;
    private final TransporterRepository transporterRepository;
    private final com.storechain.group4.repository.ShopRepository shopRepository;
    private final com.storechain.group4.repository.ProductRepository productRepository;
    private final com.storechain.group4.repository.PromotionRepository promotionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            fixPromotionsTable();
            cleanupObsoleteRoles();
            initRoles();
            initDefaultAdmin();
            initDefaultManager();
            initDefaultCategories();
            initDefaultTransporters();
            initDefaultShops();
            initDefaultProducts();
            initDefaultPromotions();
        });
    }

    private void fixPromotionsTable() {
        try {
            @SuppressWarnings("unchecked")
            List<Object> cols = entityManager.createNativeQuery(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotions'"
            ).getResultList();

            log.info("Columns found in promotions table: {}", cols);

            if (!cols.isEmpty() && !cols.contains("promotion_id")) {
                log.info("Fixing promotions table schema (missing promotion_id)...");
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
                entityManager.createNativeQuery("DROP TABLE IF EXISTS promotion_products").executeUpdate();
                entityManager.createNativeQuery("DROP TABLE IF EXISTS promotion_usages").executeUpdate();
                entityManager.createNativeQuery("DROP TABLE IF EXISTS promotions").executeUpdate();
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

                entityManager.createNativeQuery(
                        "CREATE TABLE IF NOT EXISTS promotions (" +
                        "  promotion_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "  promotion_type VARCHAR(30) NOT NULL," +
                        "  name VARCHAR(200) NOT NULL," +
                        "  description TEXT," +
                        "  code VARCHAR(50) UNIQUE," +
                        "  discount_type VARCHAR(30) NOT NULL," +
                        "  discount_value DECIMAL(12,2) NOT NULL," +
                        "  min_order_value DECIMAL(12,2)," +
                        "  max_discount_amount DECIMAL(12,2)," +
                        "  usage_limit INT," +
                        "  usage_limit_per_user INT," +
                        "  used_count INT NOT NULL DEFAULT 0," +
                        "  start_date DATETIME NOT NULL," +
                        "  end_date DATETIME NOT NULL," +
                        "  status VARCHAR(20) NOT NULL DEFAULT 'active'," +
                        "  version BIGINT DEFAULT 0," +
                        "  created_at DATETIME," +
                        "  updated_at DATETIME" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                ).executeUpdate();

                entityManager.createNativeQuery(
                        "CREATE TABLE IF NOT EXISTS promotion_usages (" +
                        "  usage_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "  promotion_id BIGINT NOT NULL," +
                        "  user_id BIGINT NOT NULL," +
                        "  discount_amount DECIMAL(12,2) NOT NULL," +
                        "  order_total DECIMAL(12,2) NOT NULL," +
                        "  used_at DATETIME," +
                        "  CONSTRAINT fk_usage_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                ).executeUpdate();

                entityManager.createNativeQuery(
                        "CREATE TABLE IF NOT EXISTS promotion_products (" +
                        "  promotion_id BIGINT NOT NULL," +
                        "  product_id BIGINT NOT NULL," +
                        "  PRIMARY KEY (promotion_id, product_id)," +
                        "  CONSTRAINT fk_promo_prod_promo FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE," +
                        "  CONSTRAINT fk_promo_prod_prod FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                ).executeUpdate();

                log.info("Successfully recreated promotions, promotion_usages, and promotion_products tables!");
            }
        } catch (Exception e) {
            log.error("Notice in fixPromotionsTable: {}", e.getMessage(), e);
        }
    }

    /**
     * Xóa các role cũ (ROLE_VENDOR, ROLE_SHIPPER) khỏi database.
     * Đổi vendor thành manager: user có role ROLE_VENDOR chuyển sang ROLE_MANAGER.
     * User có role ROLE_SHIPPER chuyển sang ROLE_USER.
     * Sau đó xóa 2 role này khỏi bảng roles để tránh lỗi deserialize enum trong JPA.
     */
    private void cleanupObsoleteRoles() {
        try {
            // Đảm bảo ROLE_MANAGER và ROLE_USER có sẵn trong bảng roles
            Role managerRole = roleRepository.findByName(RoleType.ROLE_MANAGER)
                    .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_MANAGER, "Quản lý cửa hàng")));
            Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_USER, "Người dùng / Khách hàng mua sắm")));

            // 1. Chuyển các user có role ROLE_VENDOR sang ROLE_MANAGER
            @SuppressWarnings("unchecked")
            List<Object> vendorResults = entityManager
                    .createNativeQuery("SELECT role_id FROM roles WHERE role_name = 'ROLE_VENDOR'")
                    .getResultList();

            if (!vendorResults.isEmpty()) {
                Long vendorRoleId = ((Number) vendorResults.get(0)).longValue();
                int updated = entityManager
                        .createNativeQuery("UPDATE users SET role_id = :newRoleId WHERE role_id = :oldRoleId")
                        .setParameter("newRoleId", managerRole.getId())
                        .setParameter("oldRoleId", vendorRoleId)
                        .executeUpdate();
                log.info("Đã chuyển {} người dùng từ ROLE_VENDOR sang ROLE_MANAGER", updated);

                entityManager
                        .createNativeQuery("DELETE FROM roles WHERE role_id = :roleId")
                        .setParameter("roleId", vendorRoleId)
                        .executeUpdate();
                log.info("Đã xóa role cũ ROLE_VENDOR khỏi database");
            }

            // 2. Chuyển các user có role ROLE_SHIPPER sang ROLE_USER
            @SuppressWarnings("unchecked")
            List<Object> shipperResults = entityManager
                    .createNativeQuery("SELECT role_id FROM roles WHERE role_name = 'ROLE_SHIPPER'")
                    .getResultList();

            if (!shipperResults.isEmpty()) {
                Long shipperRoleId = ((Number) shipperResults.get(0)).longValue();
                int updated = entityManager
                        .createNativeQuery("UPDATE users SET role_id = :newRoleId WHERE role_id = :oldRoleId")
                        .setParameter("newRoleId", userRole.getId())
                        .setParameter("oldRoleId", shipperRoleId)
                        .executeUpdate();
                log.info("Đã chuyển {} người dùng từ ROLE_SHIPPER sang ROLE_USER", updated);

                entityManager
                        .createNativeQuery("DELETE FROM roles WHERE role_id = :roleId")
                        .setParameter("roleId", shipperRoleId)
                        .executeUpdate();
                log.info("Đã xóa role cũ ROLE_SHIPPER khỏi database");
            }
        } catch (Exception e) {
            log.error("Lỗi trong quá trình dọn dẹp role cũ: {}", e.getMessage(), e);
        }
    }

    private void initRoles() {
        Arrays.stream(RoleType.values()).forEach(roleType -> {
            if (roleRepository.findByName(roleType).isEmpty()) {
                String description = switch (roleType) {
                    case ROLE_USER -> "Người dùng / Khách hàng mua sắm";
                    case ROLE_GUEST -> "Khách vãng lai";
                    case ROLE_MANAGER -> "Quản lý cửa hàng";
                    case ROLE_ADMIN -> "Quản trị viên hệ thống";
                };
                roleRepository.save(new Role(roleType, description));
                log.info("Khởi tạo role: {} - {}", roleType.name(), description);
            }
        });
    }

    private void initDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role ADMIN không tồn tại"));

            User admin = User.builder()
                    .username("admin")
                    .email("admin@storechain.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản trị hệ thống")
                    .phone("0123456789")
                    .status("active")
                    .role(adminRole)
                    .build();

            userRepository.save(admin);
            log.info("Tạo tài khoản Admin mặc định: username = admin, password = admin123, email = admin@storechain.com");
        }
    }

    private void initDefaultManager() {
        if (!userRepository.existsByUsername("manager")) {
            Role managerRole = roleRepository.findByName(RoleType.ROLE_MANAGER)
                    .orElseThrow(() -> new RuntimeException("Role MANAGER không tồn tại"));

            User manager = User.builder()
                    .username("manager")
                    .email("manager@storechain.com")
                    .password(passwordEncoder.encode("manager123"))
                    .fullName("Quản lý cửa hàng")
                    .phone("0987654321")
                    .status("active")
                    .role(managerRole)
                    .build();

            userRepository.save(manager);
            log.info("Tạo tài khoản Manager mặc định: username = manager, password = manager123, email = manager@storechain.com");
        }
    }

    private void initDefaultCategories() {
        if (categoryRepository.count() == 0) {
            // Danh mục gốc 1: Thời trang nam
            com.storechain.group4.model.Category men = categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Thời trang nam")
                    .slug("thoi-trang-nam")
                    .imageUrl("/img/categories/cat-1.jpg")
                    .sortOrder(1)
                    .status("active")
                    .build());

            categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Áo thun nam")
                    .slug("ao-thun-nam")
                    .parent(men)
                    .sortOrder(1)
                    .status("active")
                    .build());

            categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Áo sơ mi nam")
                    .slug("ao-so-mi-nam")
                    .parent(men)
                    .sortOrder(2)
                    .status("active")
                    .build());

            categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Quần jean nam")
                    .slug("quan-jean-nam")
                    .parent(men)
                    .sortOrder(3)
                    .status("active")
                    .build());

            categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Quần tây nam")
                    .slug("quan-tay-nam")
                    .parent(men)
                    .sortOrder(4)
                    .status("active")
                    .build());

            // Danh mục gốc 2: Phụ kiện
            com.storechain.group4.model.Category accessories = categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Phụ kiện thời trang")
                    .slug("phu-kien-thoi-trang")
                    .imageUrl("/img/categories/cat-3.jpg")
                    .sortOrder(3)
                    .status("active")
                    .build());

            categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Giày dép thời trang")
                    .slug("giay-dep-thoi-trang")
                    .parent(accessories)
                    .sortOrder(1)
                    .status("active")
                    .build());

            categoryRepository.save(com.storechain.group4.model.Category.builder()
                    .name("Thắt lưng & Ví da")
                    .slug("that-lung-vi-da")
                    .parent(accessories)
                    .sortOrder(2)
                    .status("active")
                    .build());

            log.info("Khởi tạo danh mục mặc định thành công (Thời trang nam, Thời trang nữ, Phụ kiện...)");
        }
    }

    private void initDefaultTransporters() {
        if (transporterRepository.count() == 0) {
            transporterRepository.save(Transporter.builder()
                    .name("Giao Hàng Nhanh (GHN)")
                    .phone("1900 636677")
                    .email("support@ghn.vn")
                    .baseFee(new BigDecimal("25000.00"))
                    .status("active")
                    .build());

            transporterRepository.save(Transporter.builder()
                    .name("Giao Hàng Tiết Kiệm (GHTK)")
                    .phone("1900 6092")
                    .email("cskh@giaohangtietkiem.vn")
                    .baseFee(new BigDecimal("20000.00"))
                    .status("active")
                    .build());

            transporterRepository.save(Transporter.builder()
                    .name("Viettel Post")
                    .phone("1900 8095")
                    .email("cskh@viettelpost.com.vn")
                    .baseFee(new BigDecimal("30000.00"))
                    .status("active")
                    .build());

            transporterRepository.save(Transporter.builder()
                    .name("J&T Express")
                    .phone("1900 1088")
                    .email(null)
                    .baseFee(new BigDecimal("22000.00"))
                    .status("inactive")
                    .build());

            log.info("Khởi tạo đơn vị vận chuyển mặc định thành công (GHN, GHTK, Viettel Post, J&T)");
        }
    }

    private void initDefaultShops() {
        if (shopRepository.count() == 0) {
            User defaultManager = userRepository.findByUsername("manager").orElse(null);

            shopRepository.save(com.storechain.group4.model.Shop.builder()
                    .name("Male Fashion - Chi nhánh Quận 1")
                    .slug("male-fashion-chi-nhanh-quan-1")
                    .address("123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh")
                    .phone("028 3822 1234")
                    .description("Chi nhánh trung tâm TP.HCM, trưng bày đầy đủ các bộ sưu tập thời trang nam cao cấp mới nhất.")
                    .status("active")
                    .manager(defaultManager)
                    .build());

            shopRepository.save(com.storechain.group4.model.Shop.builder()
                    .name("Male Fashion - Chi nhánh Ba Đình")
                    .slug("male-fashion-chi-nhanh-ba-dinh")
                    .address("45 Kim Mã, Phường Kim Mã, Quận Ba Đình, Hà Nội")
                    .phone("024 3747 5678")
                    .description("Chi nhánh thủ đô Hà Nội, phong cách phục vụ chuyên nghiệp, đa dạng các mẫu âu phục và phụ kiện.")
                    .status("active")
                    .manager(null)
                    .build());

            shopRepository.save(com.storechain.group4.model.Shop.builder()
                    .name("Male Fashion - Chi nhánh Hải Châu")
                    .slug("male-fashion-chi-nhanh-hai-chau")
                    .address("88 Bạch Đằng, Phường Hải Châu 1, Quận Hải Châu, Đà Nẵng")
                    .phone("0236 3848 999")
                    .description("Chi nhánh trung tâm TP. Đà Nẵng bên bờ sông Hàn thơ mộng.")
                    .status("active")
                    .manager(null)
                    .build());

            log.info("Khởi tạo danh sách chi nhánh cửa hàng mặc định thành công!");
        }
    }

    private void initDefaultProducts() {
        if (productRepository.count() == 0) {
            com.storechain.group4.model.Category cat = categoryRepository.findAll().stream().findFirst().orElse(null);
            com.storechain.group4.model.Shop shop = shopRepository.findAll().stream().findFirst().orElse(null);

            productRepository.save(com.storechain.group4.model.Product.builder()
                    .name("Áo Thun Nam Cổ Tròn Basic Cotton")
                    .slug("ao-thun-nam-co-tron-basic-cotton")
                    .description("Chất liệu 100% cotton thoáng mát, thấm hút mồ hôi, form dáng năng động trẻ trung.")
                    .price(new BigDecimal("250000.00"))
                    .imageUrl("/img/product/product-1.jpg")
                    .category(cat)
                    .shop(shop)
                    .stock(100)
                    .status("active")
                    .build());

            productRepository.save(com.storechain.group4.model.Product.builder()
                    .name("Áo Sơ Mi Nam Tay Dài Oxford")
                    .slug("ao-so-mi-nam-tay-dai-oxford")
                    .description("Phong cách lịch lãm, chất vải dày dặn ít nhăn, phù hợp công sở và sự kiện.")
                    .price(new BigDecimal("420000.00"))
                    .imageUrl("/img/product/product-2.jpg")
                    .category(cat)
                    .shop(shop)
                    .stock(50)
                    .status("active")
                    .build());

            productRepository.save(com.storechain.group4.model.Product.builder()
                    .name("Quần Jean Nam Slim Fit Co Giãn")
                    .slug("quan-jean-nam-slim-fit-co-gian")
                    .description("Dáng ôm tôn dáng, vải denim cao cấp co giãn 4 chiều mềm mại.")
                    .price(new BigDecimal("550000.00"))
                    .imageUrl("/img/product/product-3.jpg")
                    .category(cat)
                    .shop(shop)
                    .stock(80)
                    .status("active")
                    .build());

            log.info("Khởi tạo danh sách sản phẩm mẫu thành công (3 sản phẩm)!");
        }
    }

    private void initDefaultPromotions() {
        if (promotionRepository.count() == 0) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // 1. Flash Sale: Giảm 20% tự động
            com.storechain.group4.model.Promotion flashSale = com.storechain.group4.model.Promotion.builder()
                    .promotionType(com.storechain.group4.model.PromotionType.FLASH_SALE)
                    .name("Flash Sale Giảm 20% Mùa Hè")
                    .description("Chương trình Flash Sale giảm trực tiếp 20% trên giá hiển thị sản phẩm.")
                    .code(null)
                    .discountType(com.storechain.group4.model.DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .startDate(now.minusDays(1))
                    .endDate(now.plusDays(15))
                    .status("active")
                    .products(new java.util.HashSet<>(productRepository.findAll()))
                    .build();
            promotionRepository.save(flashSale);

            // 2. Voucher: Giảm 15% tối đa 50k
            com.storechain.group4.model.Promotion voucher1 = com.storechain.group4.model.Promotion.builder()
                    .promotionType(com.storechain.group4.model.PromotionType.VOUCHER)
                    .name("Mã Ưu Đãi Chào Bạn Mới 15%")
                    .description("Giảm 15% cho đơn hàng từ 200.000đ, tối đa 50.000đ.")
                    .code("WELCOME15")
                    .discountType(com.storechain.group4.model.DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("15.00"))
                    .minOrderValue(new BigDecimal("200000.00"))
                    .maxDiscountAmount(new BigDecimal("50000.00"))
                    .usageLimit(500)
                    .usageLimitPerUser(1)
                    .startDate(now.minusDays(1))
                    .endDate(now.plusDays(30))
                    .status("active")
                    .build();
            promotionRepository.save(voucher1);

            // 3. Voucher: Giảm 50.000đ cố định
            com.storechain.group4.model.Promotion voucher2 = com.storechain.group4.model.Promotion.builder()
                    .promotionType(com.storechain.group4.model.PromotionType.VOUCHER)
                    .name("Giảm Ngay 50K Đơn Từ 300K")
                    .description("Nhập mã SALE50K giảm trực tiếp 50.000đ cho đơn hàng từ 300.000đ.")
                    .code("SALE50K")
                    .discountType(com.storechain.group4.model.DiscountType.FIXED_AMOUNT)
                    .discountValue(new BigDecimal("50000.00"))
                    .minOrderValue(new BigDecimal("300000.00"))
                    .usageLimit(200)
                    .usageLimitPerUser(2)
                    .startDate(now.minusDays(1))
                    .endDate(now.plusDays(60))
                    .status("active")
                    .build();
            promotionRepository.save(voucher2);

            log.info("Khởi tạo danh sách khuyến mãi mẫu thành công (1 Flash Sale, 2 Vouchers)!");
        }
    }
}


