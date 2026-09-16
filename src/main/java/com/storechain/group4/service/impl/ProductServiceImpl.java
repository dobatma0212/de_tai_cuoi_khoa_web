package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.model.Category;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Shop;
import com.storechain.group4.repository.CategoryRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "price", "stock", "status", "createdAt"
    );

    // ==================== Vendor ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getMyProducts(Long userId, Long shopId, String keyword, String status, Long categoryId,
                                                  int page, int size, String sortBy, String sortDir) {
        Shop shop = requireMyActiveShop(userId, shopId);
        Pageable pageable = buildPageable(sortBy, sortDir, page, size);
        return productRepository.searchProducts(shop.getId(), keyword, status, categoryId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getMyProductById(Long userId, Long shopId, Long productId) {
        return toDto(requireMyProduct(userId, shopId, productId));
    }

    @Override
    @Transactional
    public ProductResponseDto createMyProduct(Long userId, Long shopId, ProductRequestDto request) {
        Shop shop = requireMyActiveShop(userId, shopId);
        Category category = requireCategory(request.getCategoryId());

        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .name(normalizeRequired(request.getName(), "Tên sản phẩm không được để trống"))
                .description(normalizeOptional(request.getDescription()))
                .price(requireValidPrice(request.getPrice()))
                .stock(requireValidStock(request.getStock()))
                .imageUrl(normalizeOptional(request.getImageUrl()))
                .status(normalizeStatus(request.getStatus()))
                .build();

        Product saved = productRepository.save(product);
        log.info("Đã tạo sản phẩm mới: ID={}, Shop={}", saved.getId(), shop.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateMyProduct(Long userId, Long shopId, Long productId, ProductRequestDto request) {
        Product product = requireMyProduct(userId, shopId, productId);

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(normalizeOptional(request.getDescription()));
        }
        if (request.getPrice() != null) {
            product.setPrice(requireValidPrice(request.getPrice()));
        }
        if (request.getStock() != null) {
            product.setStock(requireValidStock(request.getStock()));
        }
        if (request.getCategoryId() != null) {
            product.setCategory(requireCategory(request.getCategoryId()));
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(normalizeOptional(request.getImageUrl()));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            product.setStatus(normalizeStatus(request.getStatus()));
        }

        Product updated = productRepository.save(product);
        log.info("Đã cập nhật sản phẩm: ID={}", updated.getId());
        return toDto(updated);
    }

    @Override
    @Transactional
    public ProductResponseDto updateMyProductStatus(Long userId, Long shopId, Long productId, String status) {
        Product product = requireMyProduct(userId, shopId, productId);
        product.setStatus(normalizeStatus(status));
        Product saved = productRepository.save(product);
        log.info("Đã cập nhật trạng thái sản phẩm ID {} thành {}", productId, saved.getStatus());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteMyProduct(Long userId, Long shopId, Long productId) {
        Product product = requireMyProduct(userId, shopId, productId);
        productRepository.delete(product);
        log.info("Đã xóa sản phẩm ID: {}", productId);
    }

    // ==================== Public ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getPublicProducts(Long shopId, String keyword, int page, int size) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng với ID: " + shopId));
        if (!shop.isActive()) {
            throw new IllegalArgumentException("Cửa hàng này hiện không hoạt động");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Chuẩn hóa keyword: null/rỗng -> "" để tránh lỗi khi so sánh tham số NULL trong JPQL
        String kw = (keyword == null) ? "" : keyword.trim();
        return productRepository.searchPublicProducts(shopId, kw, pageable).map(this::toDto);
    }

    // ==================== Helper ====================

    private Shop requireMyActiveShop(Long userId, Long shopId) {
        Shop shop;
        if (shopId != null) {
            shop = shopRepository.findByIdAndOwnerId(shopId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cửa hàng này trong danh sách cửa hàng của bạn"));
        } else {
            List<Shop> actives = shopRepository.findAllByOwnerIdAndStatusIgnoreCaseOrderByIdDesc(userId, "active");
            shop = actives.isEmpty() ? null : actives.get(0);
        }
        if (shop == null) {
            throw new IllegalArgumentException("Bạn chưa có cửa hàng đang hoạt động");
        }
        if (!shop.isActive()) {
            throw new IllegalArgumentException("Cửa hàng của bạn chưa được duyệt hoặc đã bị khóa");
        }
        return shop;
    }

    private Product requireMyProduct(Long userId, Long shopId, Long productId) {
        Shop shop = requireMyActiveShop(userId, shopId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + productId));
        if (!product.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Sản phẩm này không thuộc cửa hàng của bạn");
        }
        return product;
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + categoryId));
    }

    private BigDecimal requireValidPrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Giá sản phẩm không được để trống");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá sản phẩm không được âm");
        }
        return price;
    }

    private Integer requireValidStock(Integer stock) {
        if (stock == null) {
            throw new IllegalArgumentException("Số lượng tồn kho không được để trống");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được âm");
        }
        return stock;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "active";
        }
        if (!"active".equalsIgnoreCase(status) && !"hidden".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'active' hoặc 'hidden'");
        }
        return status.toLowerCase();
    }

    private Pageable buildPageable(String sortBy, String sortDir, int page, int size) {
        if (sortBy == null || sortBy.isBlank() || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "id";
        }
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    private ProductResponseDto toDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .build();
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = value != null ? value.trim() : "";
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
