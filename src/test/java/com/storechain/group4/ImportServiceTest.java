package com.storechain.group4;

import com.storechain.group4.dto.ImportReceiptDto;
import com.storechain.group4.dto.ImportReceiptRequestDto;
import com.storechain.group4.model.*;
import com.storechain.group4.repository.*;
import com.storechain.group4.service.impl.ImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit test nghiệp vụ nhập hàng: cộng/trừ tồn đúng tổng và theo size,
 * không xác nhận hai lần, hủy phiếu đủ/thiếu tồn, không đụng chi nhánh khác,
 * không nhận sản phẩm không active.
 */
@ExtendWith(MockitoExtension.class)
public class ImportServiceTest {

    @Mock
    private StockReceiptRepository receiptRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private BranchStockRepository branchStockRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.storechain.group4.repository.ShopRepository shopRepository;

    @InjectMocks
    private ImportServiceImpl importService;

    private Shop shopA;
    private Shop shopB;
    private User manager;
    private Product product;
    private BranchStock stock;

    @BeforeEach
    public void setUp() {
        shopA = Shop.builder().id(1L).name("Chi nhánh A").build();
        shopB = Shop.builder().id(2L).name("Chi nhánh B").build();
        manager = User.builder().id(10L).username("manager").shop(shopA).build();
        product = Product.builder()
                .id(100L).name("Áo thun").baseSku("AT01")
                .shop(shopA).sizes("S,M").status("active")
                .category(ShopCategory.builder().id(1L).name("Áo").build())
                .build();
        // findMyShop của ImportService đọc shops.manager_id (như ManagerShopServiceImpl)
        lenient().when(shopRepository.findByManagerId(10L)).thenReturn(java.util.List.of(shopA));

        stock = BranchStock.builder().id(1000L).product(product).shop(shopA).stock(20)
                .sizeStocks("S:10,M:10").build();
    }

    private ImportReceiptRequestDto request(String size, int qty) {
        return ImportReceiptRequestDto.builder()
                .sourceNote("NCC test")
                .items(List.of(ImportReceiptRequestDto.Item.builder()
                        .productId(100L).size(size).quantity(qty).build()))
                .build();
    }

    private StockReceipt savedDraft() {
        StockReceipt draft = StockReceipt.builder()
                .id(5L).receiptCode("PN-20260930-0001").shop(shopA)
                .status(ReceiptStatus.DRAFT).createdBy(manager).totalQuantity(5)
                .build();
        draft.setItems(new java.util.ArrayList<>(List.of(
                StockReceiptItem.builder().id(50L).receipt(draft).product(product)
                        .productName("Áo thun").baseSku("AT01").size("M").quantity(5).build())));
        return draft;
    }

    @Test
    public void confirmAddsStockTotalAndPerSize() {
        StockReceipt draft = savedDraft();
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(branchStockRepository.findByProductIdAndShopIdForUpdate(100L, 1L))
                .thenReturn(Optional.of(stock));
        when(receiptRepository.save(any(StockReceipt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ImportReceiptDto dto = importService.confirmReceipt(10L, 5L);

        assertEquals("CONFIRMED", dto.getStatus());
        assertEquals(25, stock.getStock());
        assertEquals("S:10,M:15", stock.getSizeStocks());

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        StockMovement mv = captor.getValue();
        assertEquals(MovementType.IMPORT, mv.getType());
        assertEquals(5, mv.getQuantityChange());
        assertEquals(25, mv.getBalanceAfter()); // tồn sau biến động
        assertEquals("STOCK_RECEIPT", mv.getRefType());
        assertEquals(5L, mv.getRefId());
    }

    @Test
    public void confirmTwiceThrowsAndDoesNotAddTwice() {
        StockReceipt confirmed = savedDraft();
        confirmed.setStatus(ReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(confirmed));
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> importService.confirmReceipt(10L, 5L));
        assertTrue(ex.getMessage().contains("Chỉ phiếu nháp"));
        verify(branchStockRepository, never()).save(any());
        assertEquals(20, stock.getStock());
    }

    @Test
    public void createReceiptCreatesBranchStockWhenMissing() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(receiptRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(receiptRepository.existsByReceiptCode(any())).thenReturn(false);
        when(receiptRepository.save(any(StockReceipt.class)))
                .thenAnswer(inv -> {
                    StockReceipt r = inv.getArgument(0);
                    r.setId(99L);
                    return r;
                });

        ImportReceiptDto dto = importService.createReceipt(10L, request("M", 5));

        assertEquals("DRAFT", dto.getStatus());
        assertEquals(5, dto.getTotalQuantity());
        // chỉ tạo bản ghi kho khi XÁC NHẬN, không phải khi lập nháp
        verify(branchStockRepository, never()).save(any());
    }

    @Test
    public void confirmCreatesBranchStockWhenMissing() {
        StockReceipt draft = savedDraft();
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(branchStockRepository.findByProductIdAndShopIdForUpdate(100L, 1L))
                .thenReturn(Optional.empty());
        when(receiptRepository.save(any(StockReceipt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        importService.confirmReceipt(10L, 5L);

        ArgumentCaptor<BranchStock> captor = ArgumentCaptor.forClass(BranchStock.class);
        verify(branchStockRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getStock()); // 0 + 5
        assertNotNull(captor.getValue().getSizeStocks());
    }

    @Test
    public void cancelDraftOnlyChangesStatus() {
        StockReceipt draft = savedDraft();
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(draft));
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(receiptRepository.save(any(StockReceipt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ImportReceiptDto dto = importService.cancelReceipt(10L, 5L);

        assertEquals("CANCELLED", dto.getStatus());
        verify(branchStockRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    public void cancelConfirmedSubtractsStockWhenEnough() {
        StockReceipt confirmed = savedDraft();
        confirmed.setStatus(ReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(confirmed));
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(branchStockRepository.findByProductIdAndShopIdForUpdate(100L, 1L))
                .thenReturn(Optional.of(stock));
        when(receiptRepository.save(any(StockReceipt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ImportReceiptDto dto = importService.cancelReceipt(10L, 5L);

        assertEquals("CANCELLED", dto.getStatus());
        assertEquals(15, stock.getStock());
        assertEquals("S:10,M:5", stock.getSizeStocks());

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertEquals(MovementType.IMPORT_CANCEL, captor.getValue().getType());
        assertEquals(-5, captor.getValue().getQuantityChange());
    }

    @Test
    public void cancelConfirmedFailsWhenStockInsufficient() {
        StockReceipt confirmed = savedDraft();
        confirmed.setStatus(ReceiptStatus.CONFIRMED);
        // tồn hiện tại 3 < số lượng nhập 5
        stock.setStock(3);
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(confirmed));
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(branchStockRepository.findByProductIdAndShopIdForUpdate(100L, 1L))
                .thenReturn(Optional.of(stock));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> importService.cancelReceipt(10L, 5L));
        assertTrue(ex.getMessage().contains("Không thể hủy phiếu"));
        verify(branchStockRepository, never()).save(any());
    }

    @Test
    public void managerCannotTouchOtherShopReceipt() {
        StockReceipt otherShopReceipt = StockReceipt.builder()
                .id(6L).receiptCode("PN-20260930-0002").shop(shopB)
                .status(ReceiptStatus.DRAFT).createdBy(manager).build();
        when(receiptRepository.findById(6L)).thenReturn(Optional.of(otherShopReceipt));


        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> importService.getReceipt(10L, 6L));
        assertTrue(ex.getMessage().contains("không thuộc chi nhánh của bạn"));
    }

    @Test
    public void createReceiptRejectsNonActiveProduct() {
        product.setStatus("inactive");
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> importService.createReceipt(10L, request(null, 5)));
        assertTrue(ex.getMessage().contains("không thể nhập hàng"));
        verify(receiptRepository, never()).save(any());
    }

    @Test
    public void createReceiptRejectsInvalidSize() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> importService.createReceipt(10L, request("XXL", 5)));
        assertTrue(ex.getMessage().contains("phải chọn size"));
    }

    @Test
    public void createReceiptMergesDuplicateLines() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(receiptRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(receiptRepository.existsByReceiptCode(any())).thenReturn(false);
        when(receiptRepository.save(any(StockReceipt.class)))
                .thenAnswer(inv -> {
                    StockReceipt r = inv.getArgument(0);
                    r.setId(99L);
                    return r;
                });

        ImportReceiptRequestDto req = ImportReceiptRequestDto.builder()
                .items(List.of(
                        ImportReceiptRequestDto.Item.builder().productId(100L).size("M").quantity(3).build(),
                        ImportReceiptRequestDto.Item.builder().productId(100L).size("M").quantity(2).build()))
                .build();
        ImportReceiptDto dto = importService.createReceipt(10L, req);

        assertEquals(1, dto.getItems().size());
        assertEquals(5, dto.getItems().get(0).getQuantity());
        assertEquals(5, dto.getTotalQuantity());
    }

    @Test
    public void updateReceiptOnlyWhenDraft() {
        StockReceipt confirmed = savedDraft();
        confirmed.setStatus(ReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(confirmed));

        assertThrows(IllegalArgumentException.class,
                () -> importService.updateReceipt(10L, 5L, request("M", 1)));
        verify(receiptRepository, never()).save(any());
    }
}
