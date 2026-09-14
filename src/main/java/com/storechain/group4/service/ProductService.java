package com.storechain.group4.service;

import com.storechain.group4.dto.ProductHomeResponse;

import java.util.List;

public interface ProductService {

    // Danh sách sản phẩm bán chạy (đã bán trên 10 sản phẩm), sắp xếp giảm dần
    List<ProductHomeResponse> getBestSellingProducts();
}
