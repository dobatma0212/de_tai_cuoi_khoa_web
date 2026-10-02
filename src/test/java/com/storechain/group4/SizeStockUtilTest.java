package com.storechain.group4;

import com.storechain.group4.util.SizeStockUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SizeStockUtilTest {

    @Test
    public void parsesSizeStocksAndFillsMissingSizes() {
        Map<String, Integer> map = SizeStockUtil.parseSizeStocks("S:10,M:15", "S,M,L", 25);
        assertEquals(10, map.get("S"));
        assertEquals(15, map.get("M"));
        assertEquals(0, map.get("L"));
    }

    @Test
    public void addToSizeAddsToExistingSize() {
        String result = SizeStockUtil.addToSize("S:10,M:15", "S,M", 25, "M", 5);
        assertEquals("S:10,M:20", result);
    }

    @Test
    public void addToSizeSplitsEvenlyWhenCurrentIsNull() {
        // sizeStocks null (kho mới): tồn cũ 20 chia đều S=10,M=10 rồi cộng 5 vào S
        String result = SizeStockUtil.addToSize(null, "S,M", 20, "S", 5);
        assertEquals("S:15,M:10", result);
    }

    @Test
    public void addToSizeWithoutSizesUsesDefaultKey() {
        String result = SizeStockUtil.addToSize("Tiêu chuẩn:5", null, 5, null, 3);
        assertEquals("Tiêu chuẩn:8", result);
    }

    @Test
    public void subtractFromSizeReducesQuantity() {
        String result = SizeStockUtil.subtractFromSize("S:10,M:15", "S,M", 25, "M", 5);
        assertEquals("S:10,M:10", result);
    }

    @Test
    public void subtractFromSizeFailsWhenSizeStockInsufficient() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SizeStockUtil.subtractFromSize("S:10,M:2", "S,M", 12, "M", 5));
        assertTrue(ex.getMessage().contains("không đủ"));
    }

    @Test
    public void splitEvenlyHandlesRemainder() {
        Map<String, Integer> map = SizeStockUtil.splitEvenly(java.util.List.of("S", "M", "L"), 10);
        assertEquals(3, map.get("S"));
        assertEquals(3, map.get("M"));
        // size cuối chịu phần dư
        assertEquals(4, map.get("L"));
    }
}
