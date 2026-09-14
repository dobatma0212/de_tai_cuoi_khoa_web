package com.storechain.group4.service.impl;

import com.storechain.group4.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-name:StoreChain}")
    private String fromName;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, long minutesValid, String purposeText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[StoreChain] Mã OTP " + purposeText + ": " + otpCode);
            helper.setFrom(fromEmail, fromName);
            helper.setText(buildHtmlContent(otpCode, minutesValid, purposeText), true);

            mailSender.send(message);
            log.info("Đã gửi email OTP ({}) tới: {}", purposeText, toEmail);
        } catch (MessagingException e) {
            log.error("Gửi email OTP thất bại tới {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.");
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gửi email OTP tới {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.");
        }
    }

    private String buildHtmlContent(String otpCode, long minutesValid, String purposeText) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;border:1px solid #ebebeb;border-radius:8px;\">"
                + "<h2 style=\"color:#e53637;margin-bottom:4px;\">StoreChain</h2>"
                + "<p style=\"color:#5C5C5C;font-size:15px;\">Bạn (hoặc ai đó) đã yêu cầu <b>" + purposeText + "</b> cho tài khoản StoreChain gắn với email này.</p>"
                + "<div style=\"background:#f5f5f5;border-radius:6px;padding:16px;text-align:center;margin:20px 0;\">"
                + "<span style=\"font-size:32px;font-weight:700;letter-spacing:6px;color:#0c2b4b;\">" + otpCode + "</span>"
                + "</div>"
                + "<p style=\"color:#5C5C5C;font-size:14px;\">Mã có hiệu lực trong <b>" + minutesValid + " phút</b>. "
                + "Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                + "<p style=\"color:#999;font-size:12px;margin-top:24px;\">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>"
                + "</div>";
    }
}
