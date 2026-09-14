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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initDefaultAdmin();
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
}

