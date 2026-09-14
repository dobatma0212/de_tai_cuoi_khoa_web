package com.storechain.group4.controller;

import com.storechain.group4.dto.ProductHomeResponse;
import com.storechain.group4.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET /api/public/products/best-sellers
    // Trả về danh sách sản phẩm đã bán trên 10 sản phẩm, sắp xếp giảm dần theo số lượng bán
    @GetMapping("/best-sellers")
    public List<ProductHomeResponse> getBestSellers() {
        return productService.getBestSellingProducts();
    }
}
