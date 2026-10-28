package com.storechain.group4.service.impl;

import com.storechain.group4.dto.ImportItemDto;
import com.storechain.group4.dto.ImportReceiptDto;
import com.storechain.group4.dto.ImportReceiptRequestDto;
import com.storechain.group4.model.*;
import com.storechain.group4.repository.*;
import com.storechain.group4.service.ImportService;
import com.storechain.group4.util.SizeStockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportServiceImpl implements ImportService {

    private static final String REF_TYPE_RECEIPT = "STOCK_RECEIPT";

    private final StockReceiptRepository receiptRepository;
    private final StockMovementRepository movementRepository;
    private final BranchStockRepository branchStockRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final com.storechain.group4.repository.ShopRepository shopRepository;

    // ==================== Đọc ====================

    @Override
    @Transactional(readOnly = true)
    public List<ImportReceiptDto> getMyReceipts(Long managerId, String status, String fromDate, String toDate) {
        Shop shop = findMyShop(managerId);
        ReceiptStatus st = parseStatus(status);
        LocalDate from = parseDate(fromDate, "Ngày bắt đầu");
        LocalDate to = parseDate(toDate, "Ngày kết thúc");

        List<StockReceipt> receipts = (st == null)
                ? receiptRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                : receiptRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shop.getId(), st);

        return receipts.stream()
                .filter(r -> from == null
                        || (r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isBefore(from)))
                .filter(r -> to == null
                        || (r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isAfter(to)))
                .map(r -> toDto(r, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ImportReceiptDto getReceipt(Long managerId, Long receiptId) {
        Shop shop = findMyShop(managerId);
        StockReceipt receipt = findOwnedReceipt(shop, receiptId);
        return toDto(receipt, true);
    }

    // ==================== Ghi ====================

    @Override
    @Transactional
    public ImportReceiptDto createReceipt(Long managerId, ImportReceiptRequestDto request) {
        Shop shop = findMyShop(managerId);
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản lý."));

        List<StockReceiptItem> items = buildItems(shop, request);
        StockReceipt receipt = StockReceipt.builder()
                .receiptCode(generateReceiptCode())
                .shop(shop)
                .status(ReceiptStatus.DRAFT)
                .sourceNote(normalize(request.getSourceNote(), 255))
                .note(normalize(request.getNote(), 1000))
                .createdBy(manager)
                .totalQuantity(items.stream().mapToInt(StockReceiptItem::getQuantity).sum())
                .build();
        items.forEach(i -> i.setReceipt(receipt));
        receipt.getItems().addAll(items);
        return toDto(receiptRepository.save(receipt), true);
    }

    @Override
    @Transactional
    public ImportReceiptDto updateReceipt(Long managerId, Long receiptId, ImportReceiptRequestDto request) {
        Shop shop = findMyShop(managerId);
        StockReceipt receipt = findOwnedReceipt(shop, receiptId);
        requireDraft(receipt, "Chỉ phiếu nháp (DRAFT) mới được sửa.");

        List<StockReceiptItem> newItems = buildItems(shop, request);
        receipt.getItems().clear();
        newItems.forEach(i -> i.setReceipt(receipt));
        receipt.getItems().addAll(newItems);
        receipt.setSourceNote(normalize(request.getSourceNote(), 255));
        receipt.setNote(normalize(request.getNote(), 1000));
        receipt.setTotalQuantity(receipt.getItems().stream()
                .mapToInt(StockReceiptItem::getQuantity).sum());
        return toDto(receiptRepository.save(receipt), true);
    }

    @Override
    @Transactional
    public ImportReceiptDto confirmReceipt(Long managerId, Long receiptId) {
        Shop shop = findMyShop(managerId);
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản lý."));
        StockReceipt receipt = findOwnedReceipt(shop, receiptId);
        requireDraft(receipt, "Chỉ phiếu nháp (DRAFT) mới được xác nhận.");

        LocalDateTime now = LocalDateTime.now();
        for (StockReceiptItem item : receipt.getItems()) {
            applyImport(shop, item, receipt, manager, now);
        }
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt.setConfirmedBy(manager);
        receipt.setConfirmedAt(now);
        return toDto(receiptRepository.save(receipt), true);
    }

    @Override
    @Transactional
    public ImportReceiptDto cancelReceipt(Long managerId, Long receiptId) {
        Shop shop = findMyShop(managerId);
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản lý."));
        StockReceipt receipt = findOwnedReceipt(shop, receiptId);

        if (receipt.getStatus() == ReceiptStatus.CANCELLED) {
            throw new IllegalArgumentException("Phiếu này đã bị hủy trước đó.");
        }
        if (receipt.getStatus() == ReceiptStatus.DRAFT) {
            receipt.setStatus(ReceiptStatus.CANCELLED);
            receipt.setCancelledAt(LocalDateTime.now());
            return toDto(receiptRepository.save(receipt), true);
        }

        // CONFIRMED: trừ ngược tồn kho — chỉ được nếu tồn hiện tại đủ
        LocalDateTime now = LocalDateTime.now();
        for (StockReceiptItem item : receipt.getItems()) {
            applyImportCancel(shop, item, receipt, manager, now);
        }
        receipt.setStatus(ReceiptStatus.CANCELLED);
        receipt.setCancelledAt(now);
        return toDto(receiptRepository.save(receipt), true);
    }

    // ==================== Cộng / trừ tồn ====================

    /** Cộng tồn kho chi nhánh cho một dòng nhập (khi xác nhận phiếu) */
    private void applyImport(Shop shop, StockReceiptItem item, StockReceipt receipt, User manager, LocalDateTime now) {
        Product product = item.getProduct();
        BranchStock bs = branchStockRepository
                .findByProductIdAndShopIdForUpdate(product.getId(), shop.getId())
                .orElseGet(() -> BranchStock.builder()
                        .product(product).shop(shop).stock(0)
                        .sizeStocks(null).build());

        int oldStock = Math.max(0, bs.getStock());
        int newStock = oldStock + item.getQuantity();
        bs.setStock(newStock);
        bs.setSizeStocks(SizeStockUtil.addToSize(
                bs.getSizeStocks(), product.getSizes(), oldStock,
                item.getSize(), item.getQuantity()));
        bs.setUpdatedAt(now);
        branchStockRepository.save(bs);
        productRepository.syncStockFromBranches(product.getId());

        movementRepository.save(StockMovement.builder()
                .shop(shop)
                .product(product)
                .productName(item.getProductName())
                .size(item.getSize())
                .type(MovementType.IMPORT)
                .quantityChange(item.getQuantity())
                .balanceAfter(newStock)
                .refType(REF_TYPE_RECEIPT)
                .refId(receipt.getId())
                .createdBy(manager)
                .build());
    }

    /** Trừ ngược tồn kho một dòng nhập (khi hủy phiếu đã xác nhận) */
    private void applyImportCancel(Shop shop, StockReceiptItem item, StockReceipt receipt, User manager, LocalDateTime now) {
        Product product = item.getProduct();
        BranchStock bs = branchStockRepository
                .findByProductIdAndShopIdForUpdate(product.getId(), shop.getId())
                .orElse(null);
        int current = bs == null ? 0 : Math.max(0, bs.getStock());
        if (current < item.getQuantity()) {
            throw new IllegalArgumentException("Không thể hủy phiếu " + receipt.getReceiptCode()
                    + ": sản phẩm \"" + item.getProductName() + "\" chỉ còn " + current
                    + " tại chi nhánh (cần trừ " + item.getQuantity() + ").");
        }

        int newStock = current - item.getQuantity();
        bs.setStock(newStock);
        bs.setSizeStocks(SizeStockUtil.subtractFromSize(
                bs.getSizeStocks(), product.getSizes(), current,
                item.getSize(), item.getQuantity()));
        bs.setUpdatedAt(now);
        branchStockRepository.save(bs);
        productRepository.syncStockFromBranches(product.getId());

        movementRepository.save(StockMovement.builder()
                .shop(shop)
                .product(product)
                .productName(item.getProductName())
                .size(item.getSize())
                .type(MovementType.IMPORT_CANCEL)
                .quantityChange(-item.getQuantity())
                .balanceAfter(newStock)
                .refType(REF_TYPE_RECEIPT)
                .refId(receipt.getId())
                .createdBy(manager)
                .build());
    }

    // ==================== Dựng / kiểm tra dữ liệu ====================

    /**
     * Kiểm tra và gộp các dòng nhập: chỉ nhận sản phẩm active của hệ thống;
     * sản phẩm có phân size thì bắt buộc chọn size thuộc danh sách size;
     * các dòng trùng sản phẩm + size được cộng dồn.
     */
    private List<StockReceiptItem> buildItems(Shop shop, ImportReceiptRequestDto request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập phải có ít nhất một dòng hàng.");
        }
        // key: productId + "|" + size (chuẩn hóa) để gộp dòng trùng
        Map<String, StockReceiptItem> merged = new LinkedHashMap<>();
        for (ImportReceiptRequestDto.Item reqItem : request.getItems()) {
            if (reqItem.getProductId() == null) {
                throw new IllegalArgumentException("Vui lòng chọn sản phẩm cho từng dòng nhập.");
            }
            int quantity = reqItem.getQuantity() == null ? 0 : reqItem.getQuantity();
            if (quantity < 1) {
                throw new IllegalArgumentException("Số lượng nhập phải ít nhất là 1.");
            }
            Product product = productRepository.findById(reqItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong phiếu nhập."));
            if (!"active".equals(product.getStatus())) {
                throw new IllegalArgumentException("Sản phẩm \"" + product.getName()
                        + "\" đã ngừng bán hoặc đang là bản nháp, không thể nhập hàng.");
            }

            List<String> validSizes = SizeStockUtil.parseSizes(product.getSizes());
            String size = reqItem.getSize() == null || reqItem.getSize().isBlank()
                    ? null : reqItem.getSize().trim();
            if (!validSizes.isEmpty() && (size == null || !validSizes.contains(size))) {
                throw new IllegalArgumentException("Sản phẩm \"" + product.getName()
                        + "\" phải chọn size thuộc danh sách: " + String.join(", ", validSizes) + ".");
            }
            if (validSizes.isEmpty()) {
                size = null;
            }

            String key = product.getId() + "|" + (size == null ? "" : size);
            StockReceiptItem existing = merged.get(key);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + quantity);
            } else {
                merged.put(key, StockReceiptItem.builder()
                        .product(product)
                        .productName(product.getName())
                        .baseSku(product.getBaseSku())
                        .size(size)
                        .quantity(quantity)
                        .build());
            }
        }
        return new ArrayList<>(merged.values());
    }

    private StockReceipt findOwnedReceipt(Shop shop, Long receiptId) {
        StockReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhập."));
        if (!receipt.getShop().getId().equals(shop.getId())) {
            throw new IllegalArgumentException("Phiếu nhập này không thuộc chi nhánh của bạn.");
        }
        return receipt;
    }

    private void requireDraft(StockReceipt receipt, String message) {
        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new IllegalArgumentException(message + " Phiếu " + receipt.getReceiptCode()
                    + " đang ở trạng thái " + statusLabel(receipt.getStatus()) + ".");
        }
    }

    /** Sinh mã phiếu duy nhất dạng PN-yyyyMMdd-#### */
    private String generateReceiptCode() {
        LocalDate today = LocalDate.now();
        String prefix = "PN-" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long seq = receiptRepository.countByCreatedAtBetween(start, end) + 1;
        String code;
        do {
            code = prefix + String.format("%04d", seq);
            seq++;
        } while (receiptRepository.existsByReceiptCode(code));
        return code;
    }

    /**
     * Chi nhánh của manager — đọc từ shops.manager_id (admin bổ nhiệm qua trang chi nhánh),
     * giống ManagerShopServiceImpl.findMyShop để hai nơi nhìn thấy cùng một quan hệ.
     */
    private Shop findMyShop(Long managerId) {
        List<Shop> shops = shopRepository.findByManagerId(managerId);
        if (shops.isEmpty()) {
            throw new IllegalArgumentException(
                    "Bạn chưa được bổ nhiệm phụ trách chi nhánh nào.");
        }
        return shops.get(0);
    }

    private ReceiptStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return ReceiptStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái phiếu không hợp lệ: " + status);
        }
    }

    private LocalDate parseDate(String value, String label) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " không hợp lệ (dd/MM/yyyy hoặc yyyy-MM-dd).");
        }
    }

    private String normalize(String value, int maxLength) {
        String v = value == null ? null : value.trim();
        if (v != null && v.length() > maxLength) {
            throw new IllegalArgumentException("Độ dài tối đa " + maxLength + " ký tự.");
        }
        return (v == null || v.isEmpty()) ? null : v;
    }

    private String statusLabel(ReceiptStatus status) {
        return switch (status) {
            case DRAFT -> "Nháp";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
        };
    }

    // ==================== DTO ====================

    private ImportReceiptDto toDto(StockReceipt r, boolean withItems) {
        List<ImportItemDto> items = r.getItems().stream().map(this::toItemDto).toList();
        return ImportReceiptDto.builder()
                .id(r.getId())
                .receiptCode(r.getReceiptCode())
                .shopId(r.getShop().getId())
                .shopName(r.getShop().getName())
                .status(r.getStatus().name())
                .sourceNote(r.getSourceNote())
                .note(r.getNote())
                .totalQuantity(r.getTotalQuantity())
                .createdById(r.getCreatedBy() != null ? r.getCreatedBy().getId() : null)
                .createdByUsername(r.getCreatedBy() != null ? r.getCreatedBy().getUsername() : null)
                .createdByFullName(r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null)
                .confirmedByUsername(r.getConfirmedBy() != null ? r.getConfirmedBy().getUsername() : null)
                .confirmedByFullName(r.getConfirmedBy() != null ? r.getConfirmedBy().getFullName() : null)
                .createdAt(r.getCreatedAt())
                .confirmedAt(r.getConfirmedAt())
                .cancelledAt(r.getCancelledAt())
                .items(items)
                .build();
    }

    private ImportItemDto toItemDto(StockReceiptItem item) {
        return ImportItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .baseSku(item.getBaseSku())
                .size(item.getSize())
                .quantity(item.getQuantity())
                .build();
    }
}
