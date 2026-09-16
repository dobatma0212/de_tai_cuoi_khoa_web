package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.security.services.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * API tải lên hình ảnh (logo, banner, ảnh sản phẩm...).
 * Người dùng đã đăng nhập mới được tải lên; file được lưu vào thư mục cấu hình
 * "upload.dir" và được phục vụ công khai qua "/uploads/**".
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn tệp ảnh để tải lên"));
            }

            String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            String ext = original.contains(".")
                    ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận: jpg, jpeg, png, gif, webp, svg"));
            }

            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String fileName = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(fileName);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "/uploads/" + fileName;
            log.info("Người dùng {} đã tải lên ảnh: {}", currentUser.getId(), fileName);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tải ảnh lên thành công!", Map.of("url", url)));
        } catch (IOException e) {
            log.error("Lỗi khi lưu tệp ảnh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Đã xảy ra lỗi từ máy chủ (lỗi 500): không thể lưu tệp ảnh. Vui lòng thử lại sau."));
        }
    }
}
