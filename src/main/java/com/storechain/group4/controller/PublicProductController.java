package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicProductController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductResponseDto.PageDto>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "new") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        try {
            ProductResponseDto.PageDto result = productService.getProducts(keyword, category, sort, pageable);
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductBySlug(@PathVariable String slug) {
        try {
            ProductResponseDto product = productService.getProductBySlug(slug);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", product));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{slug}/related")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getRelatedProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "4") int limit) {
        try {
            List<ProductResponseDto> related = productService.getRelatedProducts(slug, Math.min(Math.max(limit, 1), 12));
            return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm liên quan thành công", related));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }
}
