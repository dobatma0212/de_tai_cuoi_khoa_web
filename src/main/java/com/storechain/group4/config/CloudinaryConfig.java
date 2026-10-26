package com.storechain.group4.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khởi tạo bean {@link Cloudinary} dùng chung cho toàn ứng dụng.
 * Thông tin xác thực đọc từ application.properties / secrets.properties / biến môi trường.
 */
@Configuration
@Slf4j
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            log.warn("Chưa cấu hình cloudinary.api-key / cloudinary.api-secret — chức năng tải ảnh lên sẽ lỗi.");
        }
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }
}
