package com.storechain.group4.service.impl;

import com.storechain.group4.dto.CreateUserRequest;
import com.storechain.group4.dto.UpdateUserRequest;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.dto.UserStatsDto;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsers(String keyword, String role, String status, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        RoleType roleType = null;
        if (role != null && !role.isBlank()) {
            try {
                String normalized = role.trim().toUpperCase();
                if (!normalized.startsWith("ROLE_")) {
                    normalized = "ROLE_" + normalized;
                }
                roleType = RoleType.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                log.warn("Vai trò không hợp lệ khi tìm kiếm: {}", role);
            }
        }

        String searchKeyword = (keyword != null && !keyword.trim().isBlank()) ? keyword.trim() : null;
        String searchStatus = (status != null && !status.trim().isBlank()) ? status.trim() : null;

        Page<User> userPage = userRepository.searchUsers(searchKeyword, roleType, searchStatus, pageable);
        return userPage.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));
        return mapToDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto createUser(CreateUserRequest request, CustomUserDetails currentUser) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã được sử dụng!");
        }

        Role assignedRole = resolveRole(request.getRole());

        if (assignedRole.getName() == RoleType.ROLE_GUEST) {
            throw new IllegalArgumentException("Không thể tạo tài khoản với vai trò Khách vãng lai (GUEST) vì khách vãng lai không sử dụng tài khoản!");
        }

        // Chỉ ADMIN mới có quyền tạo tài khoản ADMIN khác
        if (!isUserAdmin(currentUser) && assignedRole.getName() == RoleType.ROLE_ADMIN) {
            throw new SecurityException("Bạn không có quyền tạo tài khoản Quản trị viên (ADMIN)!");
        }

        String status = (request.getStatus() != null && !request.getStatus().isBlank())
                ? request.getStatus().trim().toLowerCase()
                : "active";

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl().trim() : null)
                .status(status)
                .role(assignedRole)
                .build();

        User saved = userRepository.save(user);
        log.info("Người dùng {} vừa tạo tài khoản mới: {} (vai trò: {})",
                currentUser.getUsername(), saved.getUsername(), assignedRole.getName());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequest request, CustomUserDetails currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));

        boolean isCurrentAdmin = isUserAdmin(currentUser);

        // Chỉ ADMIN mới có quyền sửa tài khoản ADMIN khác
        if (!isCurrentAdmin && user.getRole() != null && user.getRole().getName() == RoleType.ROLE_ADMIN) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa tài khoản Quản trị viên (ADMIN)!");
        }

        // Kiểm tra email trùng với tài khoản khác
        if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim())
                && userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã được sử dụng bởi người dùng khác!");
        }

        user.setEmail(request.getEmail().trim());
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        // Cập nhật vai trò
        if (request.getRole() != null && !request.getRole().isBlank()) {
            Role newRole = resolveRole(request.getRole());
            if (newRole.getName() == RoleType.ROLE_GUEST) {
                throw new IllegalArgumentException("Không thể gán vai trò Khách vãng lai (GUEST) cho tài khoản!");
            }
            // Chỉ ADMIN mới có quyền gán vai trò ADMIN
            if (!isCurrentAdmin && newRole.getName() == RoleType.ROLE_ADMIN) {
                throw new SecurityException("Bạn không có quyền gán vai trò ADMIN!");
            }
            user.setRole(newRole);
        }

        // Cập nhật trạng thái
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String newStatus = request.getStatus().trim().toLowerCase();
            // Không được tự khóa/vô hiệu hóa chính mình
            if (user.getId().equals(currentUser.getId()) && !"active".equalsIgnoreCase(newStatus)) {
                throw new IllegalArgumentException("Bạn không thể tự vô hiệu hóa hoặc khóa tài khoản của chính mình!");
            }
            user.setStatus(newStatus);
        }

        // Cập nhật mật khẩu nếu có nhập
        if (request.getPassword() != null && !request.getPassword().trim().isBlank()) {
            if (request.getPassword().trim().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        User updated = userRepository.save(user);
        log.info("Người dùng {} vừa cập nhật tài khoản ID: {}", currentUser.getUsername(), id);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public UserResponseDto updateStatus(Long id, String status, CustomUserDetails currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));

        boolean isCurrentAdmin = isUserAdmin(currentUser);

        // Chỉ ADMIN mới có quyền thay đổi trạng thái của tài khoản ADMIN khác
        if (!isCurrentAdmin && user.getRole() != null && user.getRole().getName() == RoleType.ROLE_ADMIN) {
            throw new SecurityException("Bạn không có quyền thay đổi trạng thái tài khoản ADMIN!");
        }

        String normalizedStatus = status.trim().toLowerCase();

        // Không thể tự khóa hoặc vô hiệu hóa chính mình
        if (user.getId().equals(currentUser.getId()) && !"active".equalsIgnoreCase(normalizedStatus)) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản của chính mình!");
        }

        user.setStatus(normalizedStatus);
        User saved = userRepository.save(user);
        log.info("Người dùng {} vừa đổi trạng thái tài khoản ID: {} thành '{}'",
                currentUser.getUsername(), id, normalizedStatus);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, CustomUserDetails currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));

        // Không thể tự xóa chính mình
        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không thể tự xóa tài khoản của chính mình!");
        }

        // Chỉ ADMIN mới có quyền xóa tài khoản ADMIN khác
        if (!isUserAdmin(currentUser) && user.getRole() != null && user.getRole().getName() == RoleType.ROLE_ADMIN) {
            throw new SecurityException("Bạn không có quyền xóa tài khoản Quản trị viên (ADMIN)!");
        }

        deleteUserSafely(user);

        log.info("Người dùng {} vừa xóa tài khoản: {} (ID: {})",
                currentUser.getUsername(), user.getUsername(), id);
    }

    @Override
    @Transactional
    public void deleteUserInternal(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));
        deleteUserSafely(user);
        log.info("Đã xóa hoàn toàn tài khoản người dùng: {} (ID: {})", user.getUsername(), id);
    }

    private void deleteUserSafely(User user) {
        Long id = user.getId();

        // 1. Gỡ bỏ handled_by trong bảng orders (đơn hàng do nhân viên này tiếp nhận/xử lý)
        try {
            entityManager.createNativeQuery("UPDATE orders SET handled_by = NULL WHERE handled_by = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi gỡ handled_by của user ID {}: {}", id, e.getMessage());
        }

        // 2. Xóa các bản ghi chấm công (employee_workdays) của nhân viên
        try {
            entityManager.createNativeQuery("DELETE FROM employee_workdays WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa employee_workdays của user ID {}: {}", id, e.getMessage());
        }

        // 3. Xóa các mặt hàng trong giỏ hàng (cart_items)
        try {
            entityManager.createNativeQuery("DELETE FROM cart_items WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa cart_items của user ID {}: {}", id, e.getMessage());
        }

        // 4. Xóa lịch sử tìm kiếm (search_histories)
        try {
            entityManager.createNativeQuery("DELETE FROM search_histories WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa search_histories của user ID {}: {}", id, e.getMessage());
        }

        // 5. Xóa lịch sử áp dụng khuyến mãi (promotion_usages)
        try {
            entityManager.createNativeQuery("DELETE FROM promotion_usages WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa promotion_usages của user ID {}: {}", id, e.getMessage());
        }

        // 6. Xóa hình ảnh đánh giá (review_media) và đánh giá sản phẩm (product_reviews)
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM review_media WHERE review_id IN (SELECT review_id FROM product_reviews WHERE user_id = :userId)")
                    .setParameter("userId", id)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM product_reviews WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa product_reviews/review_media của user ID {}: {}", id, e.getMessage());
        }

        // 7. Xóa địa chỉ giao hàng và địa chỉ thông thường
        try {
            entityManager.createNativeQuery("DELETE FROM shipping_addresses WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa shipping_addresses của user ID {}: {}", id, e.getMessage());
        }
        try {
            entityManager.createNativeQuery("DELETE FROM addresses WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa addresses của user ID {}: {}", id, e.getMessage());
        }

        // 8. Nếu user là quản lý chi nhánh (shops.user_id), chỉ gỡ liên kết quản lý
        try {
            entityManager.createNativeQuery("UPDATE shops SET user_id = NULL WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi gỡ quản lý chi nhánh của user ID {}: {}", id, e.getMessage());
        }

        // 9. Nếu user từng đặt đơn hàng, dọn dẹp các mục đơn hàng và đơn hàng
        try {
            entityManager.createNativeQuery(
                    "DELETE FROM order_items WHERE order_id IN (SELECT order_id FROM orders WHERE user_id = :userId)")
                    .setParameter("userId", id)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM orders WHERE user_id = :userId")
                    .setParameter("userId", id)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Lỗi khi xóa orders của user ID {}: {}", id, e.getMessage());
        }

        // 10. Xóa OTP tokens liên quan đến email của user (nếu có)
        if (user.getEmail() != null) {
            try {
                entityManager.createNativeQuery("DELETE FROM otp_tokens WHERE email = :email")
                        .setParameter("email", user.getEmail())
                        .executeUpdate();
            } catch (Exception e) {
                log.warn("Lỗi khi xóa otp_tokens của email {}: {}", user.getEmail(), e.getMessage());
            }
        }

        // 11. Cuối cùng, xóa tài khoản người dùng
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsDto getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus("active");
        long lockedUsers = userRepository.countByStatus("locked") + userRepository.countByStatus("banned");
        long adminUsers = userRepository.countByRoleType(RoleType.ROLE_ADMIN);
        long managerUsers = userRepository.countByRoleType(RoleType.ROLE_MANAGER);
        long regularUsers = userRepository.countByRoleType(RoleType.ROLE_USER);

        return UserStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .lockedUsers(lockedUsers)
                .adminUsers(adminUsers)
                .managerUsers(managerUsers)
                .regularUsers(regularUsers)
                .build();
    }

    private UserResponseDto mapToDto(User user) {
        String roleName = "N/A";
        String roleDesc = "";
        if (user.getRole() != null && user.getRole().getName() != null) {
            RoleType rt = user.getRole().getName();
            roleName = rt.name().replace("ROLE_", "");
            roleDesc = user.getRole().getDescription();
            if (roleDesc == null || roleDesc.isBlank()) {
                roleDesc = switch (rt) {
                    case ROLE_ADMIN -> "Quản trị viên hệ thống";
                    case ROLE_MANAGER -> "Quản lý chi nhánh cửa hàng";
                    case ROLE_EMPLOYEE -> "Nhân viên làm việc tại chi nhánh";
                    case ROLE_USER -> "Người dùng / Khách hàng mua sắm";
                    case ROLE_GUEST -> "Khách vãng lai";
                };
            }
        }
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .roleName(roleName)
                .roleDescription(roleDesc)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private Role resolveRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return roleRepository.findByName(RoleType.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_USER, "Người dùng / Khách hàng mua sắm")));
        }
        String normalized = roleStr.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        RoleType roleType;
        try {
            roleType = RoleType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            log.warn("Vai trò '{}' không hợp lệ, gán mặc định ROLE_USER", roleStr);
            roleType = RoleType.ROLE_USER;
        }
        final RoleType finalRoleType = roleType;
        String desc = switch (finalRoleType) {
            case ROLE_ADMIN -> "Quản trị viên hệ thống";
            case ROLE_MANAGER -> "Quản lý chi nhánh cửa hàng";
            case ROLE_EMPLOYEE -> "Nhân viên làm việc tại chi nhánh";
            case ROLE_USER -> "Người dùng / Khách hàng mua sắm";
            case ROLE_GUEST -> "Khách vãng lai";
        };
        return roleRepository.findByName(finalRoleType)
                .orElseGet(() -> roleRepository.save(new Role(finalRoleType, desc)));
    }

    private boolean isUserAdmin(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getAuthorities() == null) {
            return false;
        }
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

