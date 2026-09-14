package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ForgotPasswordRequest;
import com.storechain.group4.dto.JwtResponse;
import com.storechain.group4.dto.LoginRequest;
import com.storechain.group4.dto.RegisterRequest;
import com.storechain.group4.dto.ResendOtpRequest;
import com.storechain.group4.dto.ResetPasswordRequest;
import com.storechain.group4.dto.VerifyOtpRequest;
import com.storechain.group4.model.OtpPurpose;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.security.jwt.JwtUtils;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.AuthService;
import com.storechain.group4.service.OtpService;
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
    private final OtpService otpService;

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
                .status("pending")
                .role(assignedRole)
                .build();

        userRepository.save(user);

        otpService.generateAndSendOtp(user.getEmail(), OtpPurpose.REGISTER, "kích hoạt tài khoản");

        return ApiResponse.success(
                "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã OTP kích hoạt tài khoản.",
                user.getEmail());
    }

    @Override
    @Transactional
    public ApiResponse<String> verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        User user = userRepository.findByEmail(verifyOtpRequest.getEmail())
                .orElse(null);

        if (user == null) {
            return ApiResponse.error("Không tìm thấy tài khoản với email này.");
        }

        if (user.isAccountActive()) {
            return ApiResponse.error("Tài khoản đã được kích hoạt trước đó. Bạn có thể đăng nhập.");
        }

        boolean valid = otpService.verifyOtp(verifyOtpRequest.getEmail(), OtpPurpose.REGISTER, verifyOtpRequest.getOtp());
        if (!valid) {
            return ApiResponse.error("Mã OTP không đúng hoặc đã hết hạn.");
        }

        user.setStatus("active");
        userRepository.save(user);

        return ApiResponse.success("Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.", user.getUsername());
    }

    @Override
    public ApiResponse<String> resendOtp(ResendOtpRequest resendOtpRequest) {
        OtpPurpose purpose;
        try {
            purpose = OtpPurpose.valueOf(resendOtpRequest.getPurpose().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Mục đích OTP không hợp lệ.");
        }

        User user = userRepository.findByEmail(resendOtpRequest.getEmail()).orElse(null);
        if (user == null) {
            return ApiResponse.error("Không tìm thấy tài khoản với email này.");
        }

        if (purpose == OtpPurpose.REGISTER && user.isAccountActive()) {
            return ApiResponse.error("Tài khoản đã được kích hoạt trước đó. Bạn có thể đăng nhập.");
        }

        String purposeText = purpose == OtpPurpose.REGISTER ? "kích hoạt tài khoản" : "đặt lại mật khẩu";
        otpService.generateAndSendOtp(resendOtpRequest.getEmail(), purpose, purposeText);

        return ApiResponse.success("Mã OTP mới đã được gửi đến email của bạn.", null);
    }

    @Override
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        User user = userRepository.findByEmail(forgotPasswordRequest.getEmail()).orElse(null);
        if (user == null) {
            return ApiResponse.error("Không tìm thấy tài khoản với email này.");
        }

        otpService.generateAndSendOtp(user.getEmail(), OtpPurpose.RESET_PASSWORD, "đặt lại mật khẩu");

        return ApiResponse.success("Mã OTP đặt lại mật khẩu đã được gửi đến email của bạn.", user.getEmail());
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        User user = userRepository.findByEmail(resetPasswordRequest.getEmail()).orElse(null);
        if (user == null) {
            return ApiResponse.error("Không tìm thấy tài khoản với email này.");
        }

        boolean valid = otpService.verifyOtp(
                resetPasswordRequest.getEmail(), OtpPurpose.RESET_PASSWORD, resetPasswordRequest.getOtp());
        if (!valid) {
            return ApiResponse.error("Mã OTP không đúng hoặc đã hết hạn.");
        }

        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.success("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.", user.getUsername());
    }
}

