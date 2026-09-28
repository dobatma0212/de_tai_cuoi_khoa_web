package com.storechain.group4.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storechain.group4.dto.BranchAllocationDto;
import com.storechain.group4.dto.ProductRequestDto;
import com.storechain.group4.dto.ProductResponseDto;
import com.storechain.group4.dto.ProductStatsDto;
import com.storechain.group4.dto.ProductVariantDto;
import com.storechain.group4.dto.ShopCategoryDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.model.BranchStock;
import com.storechain.group4.model.Product;
import com.storechain.group4.model.Promotion;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.ShopCategory;
import com.storechain.group4.repository.BranchStockRepository;
import com.storechain.group4.repository.CartItemRepository;
import com.storechain.group4.repository.OrderItemRepository;
import com.storechain.group4.repository.ProductRepository;
import com.storechain.group4.repository.ProductReviewRepository;
import com.storechain.group4.repository.PromotionRepository;
import com.storechain.group4.repository.ReviewMediaRepository;
import com.storechain.group4.repository.ShopCategoryRepository;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern COMBINING_DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ShopCategoryRepository shopCategoryRepository;
    private final BranchStockRepository branchStockRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final OrderItemRepository orderItemRepository;
    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(
            String keyword,
            Long shopId,
            Long categoryId,
            String status,
            Boolean inStock,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String st = (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) ? null : status.trim();
        Long sId = (shopId == null || shopId <= 0) ? null : shopId;
        Long cId = (categoryId == null || categoryId <= 0) ? null : categoryId;

        String safeSortBy = switch (sortBy == null ? "" : sortBy.trim().toLowerCase(Locale.ROOT)) {
            case "name" -> "name";
            case "price" -> "price";
            case "stock" -> "stock";
            case "createdat", "created_at" -> "createdAt";
            default -> "id";
        };

        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(safeSortBy).ascending() : Sort.by(safeSortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        return productRepository.searchProductsAdmin(kw, sId, cId, st, minPrice, maxPrice, inStock, pageable)
                .map(this::toProductDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductStatsDto getStats() {
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByStatus("active");
        long draftProducts = productRepository.countByStatus("draft");
        long inactiveProducts = productRepository.countByStatus("inactive");
        long outOfStockProducts = productRepository.countByStockLessThanEqual(0);
        long totalStock = productRepository.sumAllStock();
        long totalShops = productRepository.countDistinctShops();
        long totalCategories = productRepository.countDistinctCategories();

        return ProductStatsDto.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .draftProducts(draftProducts)
                .inactiveProducts(inactiveProducts)
                .outOfStockProducts(outOfStockProducts)
                .totalStock(totalStock)
                .totalShops(totalShops)
                .totalCategories(totalCategories)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));
        return toProductDto(p);
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto request) {
        validateRequest(request);

        // Chi nhánh tự động liên kết với Shop mặc định đầu tiên nếu người dùng không chọn
        Shop shop = resolveShop(request.getShopId());

        ShopCategory category = shopCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));

        // Tự sinh Base SKU nếu chưa có
        String baseSku = (request.getBaseSku() != null && !request.getBaseSku().isBlank())
                ? request.getBaseSku().trim().toUpperCase(Locale.ROOT)
                : generateBaseSku(request.getName());

        // Tự sinh slug duy nhất nếu chưa có
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? toSlug(request.getSlug())
                : generateSlug(request.getName(), null);

        if (productRepository.existsBySlug(slug)) {
            slug = generateSlug(request.getName(), null);
        }

        String status = (request.getStatus() == null || request.getStatus().isBlank()) ? "active" : request.getStatus().trim();

        // Xử lý Gallery images sang JSON
        String galleryJson = serializeGallery(request.getGalleryImages());

        // Xử lý Variants sang JSON
        List<ProductVariantDto> variants = request.getVariants();
        if (variants == null || variants.isEmpty()) {
            variants = autoGenerateVariants(baseSku, request.getColor(), request.getSizes(), request.getImageUrl());
        }
        String variantsJson = serializeVariants(variants);

        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .name(request.getName().trim())
                .baseSku(baseSku)
                .brand(request.getBrand() != null ? request.getBrand().trim() : "Male Fashion")
                .collectionName(request.getCollectionName() != null ? request.getCollectionName().trim() : null)
                .slug(slug)
                .metaTitle(request.getMetaTitle() != null ? request.getMetaTitle().trim() : null)
                .metaDescription(request.getMetaDescription() != null ? request.getMetaDescription().trim() : null)
                .price(request.getPrice())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .galleryImages(galleryJson)
                .sizeChart(request.getSizeChart() != null ? request.getSizeChart().trim() : null)
                .sizes(normalizeSizes(request.getSizes()))
                .color(request.getColor() == null ? null : request.getColor().trim())
                .variantsJson(variantsJson)
                .isNew(Boolean.TRUE.equals(request.getIsNew()))
                .isBestSeller(Boolean.TRUE.equals(request.getIsBestSeller()))
                .isFeatured(Boolean.TRUE.equals(request.getIsFeatured()))
                .status(status)
                .representative(Boolean.TRUE.equals(request.getRepresentative()))
                .stock(0)
                .build();

        Product saved = productRepository.save(product);

        // Khởi tạo bản ghi kho cho tất cả các chi nhánh
        initBranchStocksForAllShops(saved, saved.getSizes());

        return toProductDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));

        validateRequest(request);

        if (request.getCategoryId() != null && (product.getCategory() == null || !request.getCategoryId().equals(product.getCategory().getId()))) {
            ShopCategory newCategory = shopCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));
            product.setCategory(newCategory);
        }

        // Cập nhật Base SKU
        if (request.getBaseSku() != null && !request.getBaseSku().isBlank()) {
            product.setBaseSku(request.getBaseSku().trim().toUpperCase(Locale.ROOT));
        }

        // Cập nhật Slug
        String slug;
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            slug = toSlug(request.getSlug());
            if (productRepository.existsBySlugAndIdNot(slug, id)) {
                slug = generateSlug(slug, id);
            }
        } else if (!product.getName().equalsIgnoreCase(request.getName().trim()) || product.getSlug() == null) {
            slug = generateSlug(request.getName(), id);
        } else {
            slug = product.getSlug();
        }

        // Xử lý Gallery images
        if (request.getGalleryImages() != null) {
            product.setGalleryImages(serializeGallery(request.getGalleryImages()));
        }

        // Xử lý Variants
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            product.setVariantsJson(serializeVariants(request.getVariants()));
        } else if (product.getVariantsJson() == null || product.getVariantsJson().isBlank()) {
            product.setVariantsJson(serializeVariants(autoGenerateVariants(
                    product.getBaseSku(), request.getColor(), request.getSizes(), request.getImageUrl())));
        }

        product.setName(request.getName().trim());
        product.setBrand(request.getBrand() != null ? request.getBrand().trim() : product.getBrand());
        product.setCollectionName(request.getCollectionName() != null ? request.getCollectionName().trim() : null);
        product.setSlug(slug);
        product.setMetaTitle(request.getMetaTitle() != null ? request.getMetaTitle().trim() : null);
        product.setMetaDescription(request.getMetaDescription() != null ? request.getMetaDescription().trim() : null);
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setSizeChart(request.getSizeChart() != null ? request.getSizeChart().trim() : product.getSizeChart());
        product.setSizes(normalizeSizes(request.getSizes()));
        product.setColor(request.getColor() == null ? null : request.getColor().trim());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            product.setStatus(request.getStatus().trim());
        }
        if (request.getIsNew() != null) product.setNew(request.getIsNew());
        if (request.getIsBestSeller() != null) product.setBestSeller(request.getIsBestSeller());
        if (request.getIsFeatured() != null) product.setFeatured(request.getIsFeatured());
        if (request.getRepresentative() != null) product.setRepresentative(request.getRepresentative());

        Product saved = productRepository.save(product);

        // Đảm bảo các chi nhánh đều có bản ghi kho
        initBranchStocksForAllShops(saved, saved.getSizes());

        return toProductDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateStatus(Long id, String status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái không được để trống.");
        }
        product.setStatus(status.trim().toLowerCase(Locale.ROOT));
        return toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponseDto updateStock(Long id, int stock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));

        int safeStock = Math.max(0, stock);
        product.setStock(safeStock);
        Product saved = productRepository.save(product);
        return toProductDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto setRepresentative(Long id, boolean representative) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));

        product.setRepresentative(representative);
        return toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponseDto toggleBadge(Long id, String badge, boolean value) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));

        switch (badge.toLowerCase(Locale.ROOT)) {
            case "new", "isnew" -> product.setNew(value);
            case "bestseller", "isbestseller" -> product.setBestSeller(value);
            case "featured", "isfeatured" -> product.setFeatured(value);
            default -> throw new IllegalArgumentException("Nhãn không hợp lệ: " + badge);
        }

        return toProductDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));

        // 1. Gỡ sản phẩm khỏi danh sách khuyến mãi
        List<Promotion> promotions = promotionRepository.findAll();
        for (Promotion promo : promotions) {
            if (promo.getProducts() != null && promo.getProducts().removeIf(p -> p.getId().equals(id))) {
                promotionRepository.save(promo);
            }
        }

        // 2. Dọn tồn kho chi nhánh
        branchStockRepository.deleteAll(branchStockRepository.findByProductId(id));

        // 3. Dọn giỏ hàng
        cartItemRepository.deleteAll(cartItemRepository.findByProductId(id));

        // 4. Dọn đánh giá & media đánh giá
        reviewRepository.findByProductIdOrderByCreatedAtDesc(id).forEach(review -> {
            reviewMediaRepository.deleteAll(reviewMediaRepository.findByReviewId(review.getId()));
            reviewRepository.delete(review);
        });

        // 5. Gỡ liên kết trong chi tiết đơn hàng
        orderItemRepository.findByProductId(id).forEach(oi -> {
            oi.setProduct(null);
            orderItemRepository.save(oi);
        });

        // 6. Xóa sản phẩm
        productRepository.delete(product);
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<ShopResponseDto> getAvailableShops() {
        return shopRepository.findAll().stream()
                .map(this::toShopDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopCategoryDto> getCategoriesByShop(Long shopId) {
        List<ShopCategory> list = (shopId == null || shopId <= 0)
                ? shopCategoryRepository.findAll()
                : shopCategoryRepository.findByShopIdOrderBySortOrderAscIdAsc(shopId);

        // Gộp danh mục trùng tên
        Map<String, ShopCategoryDto> uniqueByName = new LinkedHashMap<>();
        for (ShopCategory sc : list) {
            String nameLower = sc.getName().trim().toLowerCase(Locale.ROOT);
            if (!uniqueByName.containsKey(nameLower)) {
                uniqueByName.put(nameLower, toCategoryDto(sc));
            }
        }
        return new ArrayList<>(uniqueByName.values());
    }

    // ============ Helper Methods ============

    private void validateRequest(ProductRequestDto req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        if (req.getCategoryId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn danh mục cho sản phẩm.");
        }
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá sản phẩm phải lớn hơn hoặc bằng 0 VND.");
        }
    }

    private Shop resolveShop(Long shopId) {
        if (shopId != null && shopId > 0) {
            Optional<Shop> opt = shopRepository.findById(shopId);
            if (opt.isPresent()) return opt.get();
        }
        return shopRepository.findAll().stream().findFirst()
                .orElse(null);
    }

    private void initBranchStocksForAllShops(Product product, String sizes) {
        List<Shop> shops = shopRepository.findAll();
        for (Shop shop : shops) {
            Optional<BranchStock> existing = branchStockRepository.findByProductIdAndShopId(product.getId(), shop.getId());
            if (existing.isEmpty()) {
                BranchStock bs = BranchStock.builder()
                        .product(product)
                        .shop(shop)
                        .stock(0)
                        .sizeStocks(buildDefaultSizeStocks(sizes, 0))
                        .build();
                branchStockRepository.save(bs);
            }
        }
    }

    private String buildDefaultSizeStocks(String sizesStr, int defaultStockPerSize) {
        List<String> sizes = parseSizes(sizesStr);
        if (sizes.isEmpty()) return "All:" + defaultStockPerSize;
        return sizes.stream().map(s -> s + ":" + defaultStockPerSize).collect(Collectors.joining(","));
    }

    private Map<String, Integer> parseSizeStocks(String sizeStocksStr, String productSizes, int totalStock) {
        Map<String, Integer> map = new LinkedHashMap<>();
        List<String> expectedSizes = parseSizes(productSizes);

        if (sizeStocksStr != null && !sizeStocksStr.isBlank()) {
            String[] pairs = sizeStocksStr.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    try {
                        map.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Đảm bảo đủ các size
        if (!expectedSizes.isEmpty()) {
            for (String s : expectedSizes) {
                if (!map.containsKey(s)) {
                    map.put(s, 0);
                }
            }
        } else if (map.isEmpty()) {
            map.put("Tiêu chuẩn", totalStock);
        }
        return map;
    }

    private List<ProductVariantDto> autoGenerateVariants(String baseSku, String colorStr, String sizesStr, String mainImageUrl) {
        List<String> colors = parseCommaSeparated(colorStr);
        List<String> sizes = parseCommaSeparated(sizesStr);

        if (colors.isEmpty()) colors = List.of("Mặc định");
        if (sizes.isEmpty()) sizes = List.of("Freesize");

        String skuPrefix = (baseSku != null && !baseSku.isBlank()) ? baseSku.trim() : "SP";

        List<ProductVariantDto> list = new ArrayList<>();
        for (String c : colors) {
            String colorCode = toSlug(c).toUpperCase(Locale.ROOT).replace("-", "");
            for (String s : sizes) {
                String sizeCode = toSlug(s).toUpperCase(Locale.ROOT).replace("-", "");
                String childSku = skuPrefix + "-" + colorCode + "-" + sizeCode;
                list.add(ProductVariantDto.builder()
                        .sku(childSku)
                        .color(c)
                        .size(s)
                        .imageUrl(mainImageUrl)
                        .barcode(childSku)
                        .build());
            }
        }
        return list;
    }

    private List<String> parseCommaSeparated(String str) {
        if (str == null || str.isBlank()) return List.of();
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String generateBaseSku(String name) {
        String slug = toSlug(name).toUpperCase(Locale.ROOT);
        String[] words = slug.split("-");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(w.charAt(0));
        }
        if (sb.length() < 3) sb.append("MF");
        return sb.toString().toUpperCase(Locale.ROOT) + "-" + ((int) (Math.random() * 900) + 100);
    }

    private String serializeGallery(List<String> gallery) {
        if (gallery == null || gallery.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(gallery);
        } catch (Exception e) {
            return String.join(",", gallery);
        }
    }

    private List<String> deserializeGallery(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Arrays.stream(json.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }
    }

    private String serializeVariants(List<ProductVariantDto> variants) {
        if (variants == null || variants.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(variants);
        } catch (Exception e) {
            return null;
        }
    }

    private List<ProductVariantDto> deserializeVariants(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ProductVariantDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalizeSizes(String sizes) {
        if (sizes == null) return null;
        String cleaned = sizes.replaceAll("\\s*,\\s*", ",").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        String nowhitespace = WHITESPACE.matcher(input.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = COMBINING_DIACRITICS.matcher(normalized).replaceAll("");
        slug = slug.replace("đ", "d").replace("Đ", "d");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = slug.replaceAll("-+", "-").replaceAll("^-|-$", "");
        return slug.isBlank() ? UUID.randomUUID().toString().substring(0, 8) : slug;
    }

    private String generateSlug(String name, Long currentId) {
        String baseSlug = toSlug(name);
        String slug = baseSlug;
        int count = 1;
        while (currentId == null ? productRepository.existsBySlug(slug)
                : productRepository.existsBySlugAndIdNot(slug, currentId)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }

    private List<String> parseSizes(String sizes) {
        if (sizes == null || sizes.isBlank()) return List.of();
        return Arrays.stream(sizes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private Double round1(Double v) {
        if (v == null) return 0.0;
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private ProductResponseDto toProductDto(Product p) {
        // Aggregate tổng tồn kho từ tất cả các chi nhánh
        int totalAggregatedStock = branchStockRepository.sumStockByProductId(p.getId());

        // Bảng phân bổ tồn kho từng chi nhánh
        List<BranchStock> branchStocks = branchStockRepository.findByProductId(p.getId());
        List<BranchAllocationDto> allocations = new ArrayList<>();
        for (BranchStock bs : branchStocks) {
            allocations.add(BranchAllocationDto.builder()
                    .shopId(bs.getShop().getId())
                    .shopName(bs.getShop().getName())
                    .shopAddress(bs.getShop().getAddress())
                    .totalStock(bs.getStock())
                    .sizeStocks(parseSizeStocks(bs.getSizeStocks(), p.getSizes(), bs.getStock()))
                    .build());
        }

        List<ProductVariantDto> variants = deserializeVariants(p.getVariantsJson());
        if (variants.isEmpty()) {
            variants = autoGenerateVariants(p.getBaseSku(), p.getColor(), p.getSizes(), p.getImageUrl());
        }

        return ProductResponseDto.builder()
                .id(p.getId())
                .name(p.getName())
                .baseSku(p.getBaseSku())
                .brand(p.getBrand())
                .collectionName(p.getCollectionName())
                .shopId(p.getShop() != null ? p.getShop().getId() : null)
                .shopName(p.getShop() != null ? p.getShop().getName() : "Toàn hệ thống")
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .slug(p.getSlug())
                .metaTitle(p.getMetaTitle())
                .metaDescription(p.getMetaDescription())
                .price(p.getPrice())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .galleryImages(deserializeGallery(p.getGalleryImages()))
                .sizeChart(p.getSizeChart())
                .stock(totalAggregatedStock)
                .branchStock(totalAggregatedStock)
                .sizes(p.getSizes())
                .sizesList(parseSizes(p.getSizes()))
                .color(p.getColor())
                .variants(variants)
                .branchAllocations(allocations)
                .avgRating(round1(reviewRepository.averageRatingByProductId(p.getId())))
                .reviewCount(reviewRepository.countByProductId(p.getId()))
                .isNew(p.isNew())
                .isBestSeller(p.isBestSeller())
                .isFeatured(p.isFeatured())
                .representative(p.isRepresentative())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ShopResponseDto toShopDto(Shop s) {
        return ShopResponseDto.builder()
                .id(s.getId())
                .name(s.getName())
                .slug(s.getSlug())
                .address(s.getAddress())
                .phone(s.getPhone())
                .description(s.getDescription())
                .logoUrl(s.getLogoUrl())
                .bannerUrl(s.getBannerUrl())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .managerId(s.getManager() != null ? s.getManager().getId() : null)
                .managerUsername(s.getManager() != null ? s.getManager().getUsername() : null)
                .managerFullName(s.getManager() != null ? s.getManager().getFullName() : null)
                .build();
    }

    private ShopCategoryDto toCategoryDto(ShopCategory c) {
        return ShopCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .sortOrder(c.getSortOrder())
                .totalProducts(productRepository.countByShopIdAndCategoryId(c.getShop().getId(), c.getId()))
                .build();
    }
}
