package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.JwtResponse;
import com.storechain.group4.dto.LoginRequest;
import com.storechain.group4.dto.RegisterRequest;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.security.jwt.JwtUtils;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        String primaryRole = roles.isEmpty() ? null : roles.get(0);

        return JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .fullName(userDetails.getFullName())
                .phone(userDetails.getPhone())
                .avatarUrl(userDetails.getAvatarUrl())
                .status(userDetails.getStatus())
                .role(primaryRole)
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<String> register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ApiResponse.error("Tên đăng nhập đã được sử dụng!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ApiResponse.error("Email đã được sử dụng!");
        }

        String targetRoleStr = null;
        if (registerRequest.getRole() != null && !registerRequest.getRole().isBlank()) {
            targetRoleStr = registerRequest.getRole();
        } else if (registerRequest.getRoles() != null && !registerRequest.getRoles().isEmpty()) {
            targetRoleStr = registerRequest.getRoles().iterator().next();
        }

        Role assignedRole;
        if (targetRoleStr == null) {
            assignedRole = roleRepository.findByName(RoleType.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_USER, "Người dùng / Khách hàng mua sắm")));
        } else {
            String normalizedRole = targetRoleStr.trim().toUpperCase();
            if (!normalizedRole.startsWith("ROLE_")) {
                normalizedRole = "ROLE_" + normalizedRole;
            }

            try {
                RoleType roleType = RoleType.valueOf(normalizedRole);
                assignedRole = roleRepository.findByName(roleType)
                        .orElseGet(() -> roleRepository.save(new Role(roleType)));
            } catch (IllegalArgumentException e) {
                log.warn("Role không hợp lệ: {}, gán mặc định ROLE_USER", targetRoleStr);
                assignedRole = roleRepository.findByName(RoleType.ROLE_USER)
                        .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_USER, "Người dùng / Khách hàng mua sắm")));
            }
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .phone(registerRequest.getPhone())
                .avatarUrl(registerRequest.getAvatarUrl())
                .status("active")
                .role(assignedRole)
                .build();

        userRepository.save(user);

        return ApiResponse.success("Đăng ký tài khoản thành công!", user.getUsername());
    }
}

