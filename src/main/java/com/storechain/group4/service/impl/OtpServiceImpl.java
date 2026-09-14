package com.storechain.group4.service.impl;

import com.storechain.group4.model.OtpPurpose;
import com.storechain.group4.model.OtpToken;
import com.storechain.group4.repository.OtpTokenRepository;
import com.storechain.group4.service.MailService;
import com.storechain.group4.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpTokenRepository otpTokenRepository;
    private final MailService mailService;

    @Value("${app.otp.expiration-minutes:5}")
    private long expirationMinutes;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Override
    @Transactional
    public void generateAndSendOtp(String email, OtpPurpose purpose, String purposeText) {
        Optional<OtpToken> lastTokenOpt =
                otpTokenRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose);

        if (lastTokenOpt.isPresent()) {
            OtpToken lastToken = lastTokenOpt.get();
            long secondsSinceLast = ChronoUnit.SECONDS.between(lastToken.getCreatedAt(), LocalDateTime.now());
            if (secondsSinceLast < resendCooldownSeconds) {
                long secondsLeft = resendCooldownSeconds - secondsSinceLast;
                throw new IllegalStateException(
                        "Vui lòng đợi " + secondsLeft + " giây trước khi yêu cầu gửi lại mã OTP.");
            }
            // Vô hiệu hoá mã cũ chưa dùng
            lastToken.setUsed(true);
            otpTokenRepository.save(lastToken);
        }

        String otpCode = generateSixDigitCode();

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiryTime(LocalDateTime.now().plusMinutes(expirationMinutes))
                .used(false)
                .build();
        otpTokenRepository.save(otpToken);

        mailService.sendOtpEmail(email, otpCode, expirationMinutes, purposeText);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, OtpPurpose purpose, String otpCode) {
        Optional<OtpToken> tokenOpt =
                otpTokenRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        OtpToken token = tokenOpt.get();

        if (token.isExpired()) {
            return false;
        }

        if (!token.getOtpCode().equals(otpCode)) {
            return false;
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
        return true;
    }

    private String generateSixDigitCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
