package com.storechain.group4.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.storechain.group4.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    /**
     * Dạng URL Cloudinary:
     * https://res.cloudinary.com/{cloud}/{image|video}/upload/[v123/]{public_id}.{ext}
     */
    private static final Pattern CLOUDINARY_URL = Pattern.compile(
            "^https?://res\\.cloudinary\\.com/([^/]+)/(image|video|raw)/upload/(?:v\\d+/)?(.+?)(?:\\.[a-zA-Z0-9]+)?$");

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:storechain}")
    private String rootFolder;

    @Override
    public String upload(MultipartFile file, String subFolder, boolean video) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp để tải lên.");
        }
        String folder = (subFolder == null || subFolder.isBlank()) ? rootFolder : rootFolder + "/" + subFolder;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", video ? "video" : "image",
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false));
            Object url = result.get("secure_url");
            if (url == null) {
                throw new IllegalStateException("Cloudinary không trả về URL.");
            }
            return url.toString();
        } catch (IOException | RuntimeException e) {
            log.error("Lỗi tải tệp lên Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tải tệp lên Cloudinary, vui lòng thử lại.");
        }
    }

    @Override
    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) return;
        Matcher m = CLOUDINARY_URL.matcher(url.trim());
        if (!m.matches()) return; // không phải ảnh Cloudinary → bỏ qua
        if (!Objects.equals(m.group(1), cloudinary.config.cloudName)) return; // ảnh của cloud khác
        String resourceType = m.group(2);
        String publicId = m.group(3);
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "invalidate", true));
            log.info("Đã xóa tệp Cloudinary: {} ({})", publicId, resourceType);
        } catch (Exception e) {
            // Không làm hỏng nghiệp vụ chính chỉ vì xóa ảnh thất bại
            log.warn("Không xóa được tệp Cloudinary {}: {}", publicId, e.getMessage());
        }
    }

    @Override
    public void deleteAfterCommit(Collection<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        Set<String> targets = new LinkedHashSet<>();
        for (String u : urls) {
            if (u != null && !u.isBlank()) targets.add(u.trim());
        }
        if (targets.isEmpty()) return;
        List<String> snapshot = List.copyOf(targets);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    snapshot.forEach(CloudinaryServiceImpl.this::deleteByUrl);
                }
            });
        } else {
            snapshot.forEach(this::deleteByUrl);
        }
    }
}
