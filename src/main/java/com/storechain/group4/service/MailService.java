package com.storechain.group4.service;

public interface MailService {

    /**
     * Gửi email chứa mã OTP.
     *
     * @param toEmail     email người nhận
     * @param otpCode     mã OTP 6 số
     * @param minutesValid số phút hiệu lực của mã OTP
     * @param purposeText  mô tả mục đích (VD: "kích hoạt tài khoản", "đặt lại mật khẩu")
     */
    void sendOtpEmail(String toEmail, String otpCode, long minutesValid, String purposeText);
}
