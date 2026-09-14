package com.storechain.group4.service;

import com.storechain.group4.model.OtpPurpose;

public interface OtpService {

    /**
     * Sinh mã OTP mới cho email + mục đích, lưu vào DB và gửi mail.
     * Áp dụng cooldown chống spam (throw RuntimeException nếu gửi quá nhanh).
     */
    void generateAndSendOtp(String email, OtpPurpose purpose, String purposeText);

    /**
     * Kiểm tra mã OTP có hợp lệ (đúng, chưa dùng, chưa hết hạn) hay không.
     * Nếu hợp lệ, mã sẽ được đánh dấu "đã dùng".
     *
     * @return true nếu hợp lệ, false nếu sai/hết hạn
     */
    boolean verifyOtp(String email, OtpPurpose purpose, String otpCode);
}
