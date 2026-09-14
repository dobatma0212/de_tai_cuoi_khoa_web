package com.storechain.group4.service;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ForgotPasswordRequest;
import com.storechain.group4.dto.JwtResponse;
import com.storechain.group4.dto.LoginRequest;
import com.storechain.group4.dto.RegisterRequest;
import com.storechain.group4.dto.ResendOtpRequest;
import com.storechain.group4.dto.ResetPasswordRequest;
import com.storechain.group4.dto.VerifyOtpRequest;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    ApiResponse<String> register(RegisterRequest registerRequest);
    ApiResponse<String> verifyOtp(VerifyOtpRequest verifyOtpRequest);
    ApiResponse<String> resendOtp(ResendOtpRequest resendOtpRequest);
    ApiResponse<String> forgotPassword(ForgotPasswordRequest forgotPasswordRequest);
    ApiResponse<String> resetPassword(ResetPasswordRequest resetPasswordRequest);
}

