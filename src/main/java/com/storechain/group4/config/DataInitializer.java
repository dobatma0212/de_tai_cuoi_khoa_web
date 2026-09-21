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
    private final com.storechain.group4.repository.ShopCategoryRepository shopCategoryRepository;
    private final TransporterRepository transporterRepository;
    private final com.storechain.group4.repository.ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            cleanupObsoleteRoles();
            fixLegacySchema();
            initRoles();
            initDefaultAdmin();
            initDefaultManager();
            initDefaultCategories();
            initDefaultTransporters();
            initDefaultShops();
            initDefaultEmployee();
            initDefaultShopCategories();
            initBranchStocks();
        });
    }

    /**
     * Migration mô hình mới: sản phẩm là danh mục chung toàn hãng,
     * tồn kho được giữ theo từng chi nhánh trong bảng branch_stocks.
     * Với dữ liệu cũ, mỗi sản phẩm được gán tồn kho (cột products.stock)
     * về chi nhánh đang sở hữu sản phẩm đó.
     */
    private void initBranchStocks() {
        runStandaloneMigration(
                "INSERT INTO branch_stocks (product_id, shop_id, stock, updated_at) " +
                "SELECT p.product_id, p.shop_id, p.stock, NOW() FROM products p " +
                "WHERE NOT EXISTS (SELECT 1 FROM branch_stocks b WHERE b.product_id = p.product_id AND b.shop_id = p.shop_id)",
                "Bỏ qua migration tồn kho chi nhánh (branch_stocks)");
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

    /**
     * Sửa schema legacy còn sót từ mô hình cũ (ddl-auto=update chỉ thêm cột mới,
     * không tự xóa/sửa cột cũ nên làm hỏng INSERT):
     * 1. shops.name (cột cũ, NOT NULL) chặn INSERT vì entity giờ map shop_name -> cho phép NULL.
     * 2. products.category_id còn khóa ngoại trỏ về bảng categories (danh mục toàn hãng cũ)
     *    trong khi entity dùng shop_categories -> xóa FK legacy.
     */
    private void fixLegacySchema() {
        runStandaloneMigration("ALTER TABLE shops MODIFY `name` VARCHAR(150) NULL",
                "Bỏ qua sửa cột legacy shops.name");

        try {
            @SuppressWarnings("unchecked")
            List<Object> legacyFks = entityManager.createNativeQuery(
                    "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' " +
                    "AND COLUMN_NAME = 'category_id' AND REFERENCED_TABLE_NAME = 'categories'")
                    .getResultList();
            for (Object fkName : legacyFks) {
                runStandaloneMigration("ALTER TABLE products DROP FOREIGN KEY `" + fkName + "`",
                        "Bỏ qua xóa khóa ngoại legacy products -> categories");
                log.info("Đã xóa khóa ngoại legacy products.category_id -> categories ({})", fkName);
            }

            // Đảm bảo có FK đúng sang shop_categories (bảng danh mục chi nhánh hiện tại)
            Object fkCount = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' " +
                    "AND COLUMN_NAME = 'category_id' AND REFERENCED_TABLE_NAME = 'shop_categories'")
                    .getSingleResult();
            if (((Number) fkCount).intValue() == 0) {
                runStandaloneMigration("ALTER TABLE products ADD CONSTRAINT FK_products_shop_category " +
                        "FOREIGN KEY (category_id) REFERENCES shop_categories(category_id)",
                        "Bỏ qua thêm FK products -> shop_categories");
                log.info("Đã thêm khóa ngoại products.category_id -> shop_categories");
            }
            // cart_items: unique cũ (user_id, product_id, size) chặn lưu 2 màu khác nhau
            // cùng size -> đổi thành (user_id, product_id, size, color)
            try {
                @SuppressWarnings("unchecked")
                List<Object> oldIdx = entityManager.createNativeQuery(
                        "SELECT INDEX_NAME FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cart_items' AND NON_UNIQUE = 0 " +
                        "AND INDEX_NAME <> 'PRIMARY' " +
                        "GROUP BY INDEX_NAME " +
                        "HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'user_id,product_id,size'")
                        .getResultList();
                if (!oldIdx.isEmpty()) {
                    runStandaloneMigration(
                            "ALTER TABLE cart_items ADD CONSTRAINT UK_cart_user_product_size_color " +
                            "UNIQUE (user_id, product_id, size, color)",
                            "Bỏ qua thêm unique cart_items (user, product, size, color)");
                    for (Object idxName : oldIdx) {
                        runStandaloneMigration("ALTER TABLE cart_items DROP INDEX `" + idxName + "`",
                                "Bỏ qua xóa unique cart_items cũ");
                    }
                    log.info("Đã đổi unique cart_items sang (user_id, product_id, size, color)");
                }
            } catch (Exception e) {
                log.info("Bỏ qua kiểm tra unique cart_items: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.info("Bỏ qua kiểm tra khóa ngoại legacy của products: {}", e.getMessage());
        }
    }

    private void initRoles() {
        Arrays.stream(RoleType.values()).forEach(roleType -> {
            if (roleRepository.findByName(roleType).isEmpty()) {
                String description = switch (roleType) {
                    case ROLE_USER -> "Người dùng / Khách hàng mua sắm";
                    case ROLE_GUEST -> "Khách vãng lai";
                    case ROLE_MANAGER -> "Quản lý cửa hàng";
                    case ROLE_EMPLOYEE -> "Nhân viên chi nhánh";
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

    /** Nhân viên mẫu, gán vào chi nhánh đầu tiên (Quận 1) */
    private void initDefaultEmployee() {
        if (!userRepository.existsByUsername("employee")) {
            Role employeeRole = roleRepository.findByName(RoleType.ROLE_EMPLOYEE)
                    .orElseThrow(() -> new RuntimeException("Role EMPLOYEE không tồn tại"));

            var shops = shopRepository.findAll();
            var shop = shops.isEmpty() ? null : shops.get(0);

            User employee = User.builder()
                    .username("employee")
                    .email("employee@storechain.com")
                    .password(passwordEncoder.encode("employee123"))
                    .fullName("Nhân viên chi nhánh")
                    .phone("0901234567")
                    .status("active")
                    .role(employeeRole)
                    .shop(shop)
                    .build();

            userRepository.save(employee);
            log.info("Tạo tài khoản Employee mặc định: username = employee, password = employee123, chi nhánh = {}",
                    shop != null ? shop.getName() : "(chưa gán)");
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

    /**
     * Mỗi chi nhánh có sẵn 2 danh mục mặc định (Bộ áo quần, Phụ kiện);
     * các sản phẩm cũ chưa gắn danh mục sẽ được ghép theo cột category_group cũ.
     */
    private void initDefaultShopCategories() {
        var shops = shopRepository.findAll();
        boolean changed = false;
        for (var shop : shops) {
            var catRepo = shopCategoryRepository;
            if (catRepo.countByShopId(shop.getId()) == 0) {
                catRepo.save(com.storechain.group4.model.ShopCategory.builder()
                        .shop(shop).name("Bộ áo quần").sortOrder(0).build());
                catRepo.save(com.storechain.group4.model.ShopCategory.builder()
                        .shop(shop).name("Phụ kiện").sortOrder(1).build());
                changed = true;
            }
        }
        if (changed) {
            log.info("Đã tạo danh mục mặc định (Bộ áo quần, Phụ kiện) cho các chi nhánh.");
        }
        // Ghép sản phẩm cũ (category_id NULL hoặc 0) vào danh mục theo category_group cũ.
        // Mỗi lệnh chạy trong transaction riêng: nếu cột category_group đã bị xóa khỏi DB
        // thì chỉ bỏ qua lệnh đó mà không làm hỏng transaction chính của ứng dụng.
        runStandaloneMigration(
                "UPDATE products p JOIN shop_categories sc ON sc.shop_id = p.shop_id AND sc.name = 'Bộ áo quần' " +
                "SET p.category_id = sc.category_id WHERE (p.category_id IS NULL OR p.category_id = 0) " +
                "AND p.category_group IN ('CLOTHING', 'Bộ áo quần')",
                "Bỏ qua ghép sản phẩm theo category_group (CLOTHING)");
        runStandaloneMigration(
                "UPDATE products p JOIN shop_categories sc ON sc.shop_id = p.shop_id AND sc.name = 'Phụ kiện' " +
                "SET p.category_id = sc.category_id WHERE (p.category_id IS NULL OR p.category_id = 0) " +
                "AND p.category_group IN ('ACCESSORY', 'Phụ kiện')",
                "Bỏ qua ghép sản phẩm theo category_group (ACCESSORY)");
        // Sản phẩm còn lại: gán vào danh mục đầu tiên của chi nhánh
        runStandaloneMigration(
                "UPDATE products p JOIN (" +
                "SELECT sc.shop_id, MIN(sc.category_id) AS cid FROM shop_categories sc GROUP BY sc.shop_id" +
                ") t ON t.shop_id = p.shop_id " +
                "SET p.category_id = t.cid WHERE p.category_id IS NULL OR p.category_id = 0",
                "Bỏ qua gán danh mục cho sản phẩm chưa phân loại");
    }

    /** Chạy 1 câu SQL migration trong transaction độc lập (REQUIRES_NEW), lỗi thì chỉ ghi log rồi bỏ qua */
    private void runStandaloneMigration(String sql, String skipMessage) {
        try {
            var template = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            // REQUIRES_NEW: tách khỏi transaction lớn của DataInitializer để lỗi không lây lan
            template.setPropagationBehavior(
                    org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.executeWithoutResult(status ->
                    entityManager.createNativeQuery(sql).executeUpdate());
        } catch (Exception e) {
            log.info("{}: {}", skipMessage, e.getMessage());
        }
    }
}


