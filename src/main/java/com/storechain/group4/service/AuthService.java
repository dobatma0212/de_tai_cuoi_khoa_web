package com.storechain.group4.service;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.JwtResponse;
import com.storechain.group4.dto.LoginRequest;
import com.storechain.group4.dto.RegisterRequest;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    ApiResponse<String> register(RegisterRequest registerRequest);
}

