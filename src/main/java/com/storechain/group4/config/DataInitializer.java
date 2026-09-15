package com.storechain.group4.config;

import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final com.storechain.group4.repository.CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initDefaultAdmin();
        initDefaultManager();
        initDefaultCategories();
    }

    private void initRoles() {
        Arrays.stream(RoleType.values()).forEach(roleType -> {
            if (roleRepository.findByName(roleType).isEmpty()) {
                String description = switch (roleType) {
                    case ROLE_USER -> "Người dùng / Khách hàng mua sắm";
                    case ROLE_GUEST -> "Khách vãng lai";
                    case ROLE_VENDOR -> "Nhà bán hàng / Đối tác";
                    case ROLE_MANAGER -> "Quản lý chuỗi cửa hàng";
                    case ROLE_ADMIN -> "Quản trị viên hệ thống";
                    case ROLE_SHIPPER -> "Nhân viên giao hàng";
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
}

