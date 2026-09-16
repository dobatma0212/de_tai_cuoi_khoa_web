package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopHomeDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.ShopCategory;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ShopCategoryRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.service.ManagerShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerShopServiceImpl implements ManagerShopService {

    private final ShopRepository shopRepository;
    private final ShopCategoryRepository shopCategoryRepository;
    private final ProductRepository productRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final String DEFAULT_CATEGORY_CLOTHING = "Bộ áo quần";
    private static final String DEFAULT_CATEGORY_ACCESSORY = "Phụ kiện";
    private static final int MAX_CATEGORIES_PER_SHOP = 20;

    // ============ Chi nhánh của manager ============

    @Override
    @Transactional(readOnly = true)
    public ShopResponseDto getMyShop(Long managerId) {
        Shop shop = findMyShop(managerId);
        return toShopDto(shop);
    }

    @Override
    @Transactional
    public ShopResponseDto updateMyShop(Long managerId, ShopResponseDto request) {
        Shop shop = findMyShop(managerId);
        if (request.getName() != null && !request.getName().isBlank()) {
            shop.setName(request.getName().trim());
        }
        if (request.getAddress() != null) shop.setAddress(request.getAddress().trim());
        if (request.getPhone() != null) shop.setPhone(request.getPhone().trim());
        if (request.getDescription() != null) shop.setDescription(request.getDescription().trim());
        if (request.getLogoUrl() != null) shop.setLogoUrl(request.getLogoUrl().trim());
        if (request.getBannerUrl() != null) shop.setBannerUrl(request.getBannerUrl().trim());
        return toShopDto(shopRepository.save(shop));
    }

    private Shop findMyShop(Long managerId) {
        List<Shop> shops = shopRepository.findByManagerId(managerId);
        if (shops.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn chưa được admin bổ nhiệm phụ trách chi nhánh nào. Vui lòng liên hệ quản trị viên.");
        }
        return shops.get(0);
    }

    // ============ Danh mục sản phẩm ============

    @Override
    @Transactional
    public List<ShopCategoryDto> getMyCategories(Long managerId) {
        Shop shop = findMyShop(managerId);
        ensureDefaultCategories(shop);
        return toCategoryDtos(shop.getId());
    }

    @Override
    @Transactional
    public ShopCategoryDto addCategory(Long managerId, String name) {
        Shop shop = findMyShop(managerId);
        String trimmed = validateCategoryName(name);
        if (shopCategoryRepository.countByShopId(shop.getId()) >= MAX_CATEGORIES_PER_SHOP) {
            throw new IllegalArgumentException("Mỗi chi nhánh chỉ được tạo tối đa " + MAX_CATEGORIES_PER_SHOP + " danh mục.");
        }
        if (shopCategoryRepository.existsByShopIdAndNameIgnoreCase(shop.getId(), trimmed)) {
            throw new IllegalArgumentException("Danh mục \"" + trimmed + "\" đã tồn tại trong chi nhánh của bạn.");
        }
        ShopCategory saved = shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop)
                .name(trimmed)
                .sortOrder((int) shopCategoryRepository.countByShopId(shop.getId()))
                .build());
        return toCategoryDto(saved);
    }

    @Override
    @Transactional
    public ShopCategoryDto renameCategory(Long managerId, Long categoryId, String name) {
        Shop shop = findMyShop(managerId);
        ShopCategory category = findOwnedCategory(shop, categoryId);
        String trimmed = validateCategoryName(name);
        if (shopCategoryRepository.existsByShopIdAndNameIgnoreCaseAndIdNot(shop.getId(), trimmed, categoryId)) {
            throw new IllegalArgumentException("Danh mục \"" + trimmed + "\" đã tồn tại trong chi nhánh của bạn.");
        }
        category.setName(trimmed);
        return toCategoryDto(shopCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long managerId, Long categoryId) {
        Shop shop = findMyShop(managerId);
        ShopCategory category = findOwnedCategory(shop, categoryId);
        long productCount = productRepository.countByShopIdAndCategoryId(shop.getId(), categoryId);
        if (productCount > 0) {
            throw new IllegalArgumentException(
                    "Không thể xóa danh mục \"" + category.getName() + "\" vì còn " + productCount
                            + " sản phẩm. Hãy xóa/chuyển các sản phẩm trước.");
        }
        shopCategoryRepository.delete(category);
    }

    private String validateCategoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("Tên danh mục tối đa 100 ký tự.");
        }
        return trimmed;
    }

    private ShopCategory findOwnedCategory(Shop shop, Long categoryId) {
        return shopCategoryRepository.findByIdAndShopId(categoryId, shop.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục trong chi nhánh của bạn."));
    }

    /** Chi nhánh mới chưa có danh mục thì tạo sẵn 2 danh mục mặc định */
    private void ensureDefaultCategories(Shop shop) {
        if (shopCategoryRepository.countByShopId(shop.getId()) > 0) {
            return;
        }
        shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop).name(DEFAULT_CATEGORY_CLOTHING).sortOrder(0).build());
        shopCategoryRepository.save(ShopCategory.builder()
                .shop(shop).name(DEFAULT_CATEGORY_ACCESSORY).sortOrder(1).build());
        log.info("Đã tạo danh mục mặc định cho chi nhánh ID: {}", shop.getId());
    }

    private List<ShopCategoryDto> toCategoryDtos(Long shopId) {
        return shopCategoryRepository.findByShopIdOrderBySortOrderAscIdAsc(shopId)
                .stream().map(this::toCategoryDto).toList();
    }

    private ShopCategoryDto toCategoryDto(ShopCategory c) {
        return ShopCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .sortOrder(c.getSortOrder())
                .totalProducts(productRepository.countByShopIdAndCategoryId(c.getShop().getId(), c.getId()))
                .build();
    }

    // ============ Sản phẩm chi nhánh ============

    @Override
    @Transactional
    public List<ProductResponseDto> getMyProducts(Long managerId, Long categoryId) {
        Shop shop = findMyShop(managerId);
        ensureDefaultCategories(shop);
        List<Product> products = (categoryId == null || categoryId <= 0)
                ? productRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                : productRepository.findByShopIdAndCategoryIdOrderByCreatedAtDesc(shop.getId(), categoryId);
        return products.stream().map(this::toProductDto).toList();
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(Long managerId, ProductRequestDto request) {
        Shop shop = findMyShop(managerId);
        ensureDefaultCategories(shop);
        validatePrice(request);
        ShopCategory category = findOwnedCategory(shop, request.getCategoryId());
        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .name(request.getName().trim())
                .price(request.getPrice())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();
        Product saved = productRepository.save(product);
        // Sản phẩm đầu tiên của danh mục tự động làm đại diện
        if (!productRepository.existsByShopIdAndCategoryIdAndRepresentativeTrue(
                shop.getId(), category.getId())) {
            saved.setRepresentative(true);
            saved = productRepository.save(saved);
        }
        return toProductDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long managerId, Long productId, ProductRequestDto request) {
        Product product = findOwnedProduct(managerId, productId);
        validatePrice(request);
        ShopCategory newCategory = findOwnedCategory(product.getShop(), request.getCategoryId());
        boolean categoryChanged = !product.getCategory().getId().equals(newCategory.getId());
        product.setCategory(newCategory);
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        if (categoryChanged) {
            product.setRepresentative(false);
            Product saved = productRepository.save(product);
            if (!productRepository.existsByShopIdAndCategoryIdAndRepresentativeTrue(
                    saved.getShop().getId(), newCategory.getId())) {
                saved.setRepresentative(true);
                saved = productRepository.save(saved);
            }
            return toProductDto(saved);
        }
        return toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long managerId, Long productId) {
        Product product = findOwnedProduct(managerId, productId);
        boolean wasRepresentative = product.isRepresentative();
        Long categoryId = product.getCategory().getId();
        Long shopId = product.getShop().getId();
        productRepository.delete(product);

        // Nếu xóa sản phẩm đại diện thì chọn sản phẩm khác thay thế
        if (wasRepresentative) {
            List<Product> remaining = productRepository
                    .findByShopIdAndCategoryIdOrderByCreatedAtDesc(shopId, categoryId);
            if (!remaining.isEmpty()) {
                Product next = remaining.get(0);
                next.setRepresentative(true);
                productRepository.save(next);
            }
        }
    }

    @Override
    @Transactional
    public ProductResponseDto setRepresentative(Long managerId, Long productId, boolean representative) {
        Product product = findOwnedProduct(managerId, productId);
        if (representative) {
            // Bỏ cờ đại diện của các sản phẩm khác trong cùng danh mục
            productRepository.findByShopIdAndCategoryIdAndRepresentativeTrue(
                            product.getShop().getId(), product.getCategory().getId())
                    .forEach(p -> {
                        if (!p.getId().equals(productId)) {
                            p.setRepresentative(false);
                            productRepository.save(p);
                        }
                    });
            product.setRepresentative(true);
        } else {
            product.setRepresentative(false);
        }
        return toProductDto(productRepository.save(product));
    }

    private void validatePrice(ProductRequestDto request) {
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá sản phẩm phải là số VND hợp lệ (>= 0).");
        }
    }

    private Product findOwnedProduct(Long managerId, Long productId) {
        Shop shop = findMyShop(managerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!product.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Sản phẩm này không thuộc chi nhánh bạn quản lý.");
        }
        return product;
    }

    // ============ Upload ảnh ============

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp hình ảnh để tải lên.");
        }
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase(Locale.ROOT);
        }
        List<String> allowed = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận hình ảnh JPG, PNG, GIF, WEBP.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước ảnh tối đa là 5MB.");
        }
        try {
            Path dir = Paths.get(uploadDir, "products");
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/products/" + filename;
        } catch (IOException e) {
            log.error("Lỗi lưu tệp ảnh tải lên: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lưu tệp ảnh, vui lòng thử lại.");
        }
    }

    // ============ Trang chủ chi nhánh (public) ============

    @Override
    @Transactional
    public ShopHomeDto getShopHome(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh."));
        ensureDefaultCategories(shop);
        List<ShopHomeDto.CategoryBlock> blocks = new ArrayList<>();
        for (ShopCategory category : shopCategoryRepository
                .findByShopIdOrderBySortOrderAscIdAsc(shop.getId())) {
            List<Product> products = productRepository
                    .findByShopIdAndCategoryIdOrderByCreatedAtDesc(shop.getId(), category.getId());
            Product rep = products.stream().filter(Product::isRepresentative).findFirst()
                    .orElse(products.stream().findFirst().orElse(null));
            blocks.add(ShopHomeDto.CategoryBlock.builder()
                    .categoryId(category.getId())
                    .label(category.getName())
                    .representativeProduct(rep == null ? null : toProductDto(rep))
                    .totalProducts(products.size())
                    .build());
        }
        return ShopHomeDto.builder()
                .shop(toShopDto(shop))
                .categories(blocks)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> searchShopProducts(Long shopId, String keyword, Long categoryId) {
        String kw = keyword == null ? "" : keyword.trim();
        List<Product> products;
        if (categoryId != null && categoryId > 0) {
            products = productRepository.findByShopIdAndCategoryIdOrderByCreatedAtDesc(shopId, categoryId);
        } else {
            products = productRepository.searchProducts(shopId, kw);
        }
        return products.stream()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                .filter(p -> kw.isEmpty()
                        || p.getName().toLowerCase(Locale.ROOT).contains(kw.toLowerCase(Locale.ROOT))
                        || (p.getDescription() != null && p.getDescription()
                                .toLowerCase(Locale.ROOT).contains(kw.toLowerCase(Locale.ROOT))))
                .map(this::toProductDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> suggestShopProducts(Long shopId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return productRepository.suggestByName(shopId, keyword.trim())
                .stream().limit(8).map(this::toProductDto).toList();
    }

    // ============ Mapper ============

    private ShopResponseDto toShopDto(Shop shop) {
        return ShopResponseDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .address(shop.getAddress())
                .phone(shop.getPhone())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .status(shop.getStatus())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .managerId(shop.getManager() != null ? shop.getManager().getId() : null)
                .managerUsername(shop.getManager() != null ? shop.getManager().getUsername() : null)
                .managerFullName(shop.getManager() != null ? shop.getManager().getFullName() : null)
                .build();
    }

    private ProductResponseDto toProductDto(Product p) {
        return ProductResponseDto.builder()
                .id(p.getId())
                .shopId(p.getShop().getId())
                .shopName(p.getShop().getName())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .name(p.getName())
                .price(p.getPrice())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
