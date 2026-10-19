package com.storechain.group4.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

/**
 * Dịch vụ lưu trữ ảnh/video trên Cloudinary.
 */
public interface CloudinaryService {

    /**
     * Tải tệp lên Cloudinary.
     *
     * @param file      tệp người dùng gửi lên
     * @param subFolder thư mục con (vd: "products", "reviews") bên trong thư mục gốc cấu hình
     * @param video     true nếu là video, false nếu là ảnh
     * @return URL https công khai của tệp trên Cloudinary
     */
    String upload(MultipartFile file, String subFolder, boolean video);

    /**
     * Xóa tệp trên Cloudinary theo URL. Bỏ qua nếu URL không thuộc Cloudinary
     * (vd: ảnh cũ lưu cục bộ /uploads/..., ảnh mẫu /img/...).
     */
    void deleteByUrl(String url);

    /**
     * Xóa danh sách tệp sau khi transaction hiện tại commit thành công
     * (nếu không có transaction thì xóa ngay).
     */
    void deleteAfterCommit(Collection<String> urls);
}
