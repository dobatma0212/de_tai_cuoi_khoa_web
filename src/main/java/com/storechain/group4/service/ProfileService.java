package com.storechain.group4.service;

import com.storechain.group4.dto.ChangePasswordRequest;
import com.storechain.group4.dto.ShippingAddressRequest;
import com.storechain.group4.dto.ShippingAddressResponse;
import com.storechain.group4.dto.UpdateProfileRequest;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.security.services.CustomUserDetails;

import java.util.List;

public interface ProfileService {

    UserResponseDto getMyProfile(CustomUserDetails currentUser);

    UserResponseDto updateMyProfile(UpdateProfileRequest request, CustomUserDetails currentUser);

    void changeMyPassword(ChangePasswordRequest request, CustomUserDetails currentUser);

    List<ShippingAddressResponse> getMyAddresses(CustomUserDetails currentUser);

    ShippingAddressResponse addMyAddress(ShippingAddressRequest request, CustomUserDetails currentUser);

    ShippingAddressResponse updateMyAddress(Long addressId, ShippingAddressRequest request, CustomUserDetails currentUser);

    void deleteMyAddress(Long addressId, CustomUserDetails currentUser);

    ShippingAddressResponse setDefaultMyAddress(Long addressId, CustomUserDetails currentUser);
}
