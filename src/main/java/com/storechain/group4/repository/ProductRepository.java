package com.storechain.group4.repository;

import com.storechain.group4.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Lấy các sản phẩm đã bán trên "soldQuantity" sản phẩm, sắp xếp giảm dần theo số lượng bán
    List<Product> findBySoldQuantityGreaterThanOrderBySoldQuantityDesc(Integer soldQuantity);
}
