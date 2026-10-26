package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ChangePasswordRequest;
import com.storechain.group4.dto.ShippingAddressRequest;
import com.storechain.group4.dto.ShippingAddressResponse;
import com.storechain.group4.dto.UpdateProfileRequest;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.model.ShippingAddress;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.ShippingAddressRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    public static final int MAX_ADDRESSES_PER_USER = 10;

    private final UserRepository userRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== Thông tin cá nhân ====================

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getMyProfile(CustomUserDetails currentUser) {
        User user = getActiveUser(currentUser.getId());
        return mapUserToDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateMyProfile(UpdateProfileRequest request, CustomUserDetails currentUser) {
        User user = getActiveUser(currentUser.getId());

        String newEmail = request.getEmail().trim();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email '" + newEmail + "' đã được sử dụng bởi người dùng khác!");
        }

        user.setEmail(newEmail);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone().trim() : null);
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User updated = userRepository.save(user);
        log.info("Người dùng '{}' vừa cập nhật hồ sơ cá nhân", updated.getUsername());
        return mapUserToDto(updated);
    }

    // ==================== Đổi mật khẩu ====================

    @Override
    @Transactional
    public void changeMyPassword(ChangePasswordRequest request, CustomUserDetails currentUser) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới và mật khẩu nhập lại không khớp!");
        }

        User user = getActiveUser(currentUser.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng!");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Người dùng '{}' vừa đổi mật khẩu", user.getUsername());
    }

    // ==================== Địa chỉ nhận hàng ====================

    @Override
    @Transactional(readOnly = true)
    public List<ShippingAddressResponse> getMyAddresses(CustomUserDetails currentUser) {
        return shippingAddressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapAddressToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShippingAddressResponse addMyAddress(ShippingAddressRequest request, CustomUserDetails currentUser) {
        User user = getActiveUser(currentUser.getId());

        if (shippingAddressRepository.countByUserId(user.getId()) >= MAX_ADDRESSES_PER_USER) {
            throw new IllegalArgumentException("Bạn chỉ có thể lưu tối đa " + MAX_ADDRESSES_PER_USER + " địa chỉ nhận hàng!");
        }

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || shippingAddressRepository.countByUserId(user.getId()) == 0;

        if (makeDefault) {
            shippingAddressRepository.clearDefaultByUserId(user.getId());
        }

        ShippingAddress address = ShippingAddress.builder()
                .user(user)
                .receiverName(request.getReceiverName().trim())
                .receiverPhone(request.getReceiverPhone().trim())
                .province(request.getProvince().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard() != null && !request.getWard().isBlank() ? request.getWard().trim() : null)
                .detailAddress(request.getDetailAddress().trim())
                .isDefault(makeDefault)
                .build();

        ShippingAddress saved = shippingAddressRepository.save(address);
        log.info("Người dùng '{}' vừa thêm địa chỉ nhận hàng ID: {}", user.getUsername(), saved.getId());
        return mapAddressToDto(saved);
    }

    @Override
    @Transactional
    public ShippingAddressResponse updateMyAddress(Long addressId, ShippingAddressRequest request, CustomUserDetails currentUser) {
        ShippingAddress address = getOwnedAddress(addressId, currentUser.getId());

        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(request.getReceiverPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard() != null && !request.getWard().isBlank() ? request.getWard().trim() : null);
        address.setDetailAddress(request.getDetailAddress().trim());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            shippingAddressRepository.clearDefaultByUserId(currentUser.getId());
            address.setIsDefault(true);
        }

        ShippingAddress updated = shippingAddressRepository.save(address);
        log.info("Người dùng '{}' vừa cập nhật địa chỉ nhận hàng ID: {}", currentUser.getUsername(), addressId);
        return mapAddressToDto(updated);
    }

    @Override
    @Transactional
    public void deleteMyAddress(Long addressId, CustomUserDetails currentUser) {
        ShippingAddress address = getOwnedAddress(addressId, currentUser.getId());
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        shippingAddressRepository.delete(address);

        // Nếu địa chỉ bị xóa là mặc định, chuyển mặc định sang địa chỉ còn lại cũ nhất
        if (wasDefault) {
            shippingAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(currentUser.getId())
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsDefault(true);
                        shippingAddressRepository.save(next);
                    });
        }
        log.info("Người dùng '{}' vừa xóa địa chỉ nhận hàng ID: {}", currentUser.getUsername(), addressId);
    }

    @Override
    @Transactional
    public ShippingAddressResponse setDefaultMyAddress(Long addressId, CustomUserDetails currentUser) {
        ShippingAddress address = getOwnedAddress(addressId, currentUser.getId());

        shippingAddressRepository.clearDefaultByUserId(currentUser.getId());
        address.setIsDefault(true);
        ShippingAddress updated = shippingAddressRepository.save(address);

        log.info("Người dùng '{}' vừa đặt địa chỉ ID: {} làm mặc định", currentUser.getUsername(), addressId);
        return mapAddressToDto(updated);
    }

    // ==================== Helper ====================

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));
        if (!user.isAccountActive()) {
            throw new SecurityException("Tài khoản của bạn đang bị khóa hoặc vô hiệu hóa!");
        }
        return user;
    }

    private ShippingAddress getOwnedAddress(Long addressId, Long userId) {
        return shippingAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy địa chỉ nhận hàng với ID: " + addressId + " hoặc địa chỉ không thuộc về bạn!"));
    }

    private UserResponseDto mapUserToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .roleName(user.getRole() != null ? user.getRole().getName().name() : "N/A")
                .roleDescription(user.getRole() != null ? user.getRole().getDescription() : "")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private ShippingAddressResponse mapAddressToDto(ShippingAddress address) {
        return ShippingAddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
