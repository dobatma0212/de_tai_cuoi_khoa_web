package com.storechain.group4.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc/ghi chuỗi phân bổ tồn kho theo size của branch_stocks (format "S:10,M:15,L:12").
 * Tách khỏi ProductServiceImpl để nhập hàng (ImportService) dùng chung cách chia đều:
 * khi sizeStocks đang null thì lượng mới được chia đều cho các size.
 */
public final class SizeStockUtil {

    private SizeStockUtil() {
    }

    /** Danh sách size từ chuỗi "S,M,L" của products.sizes */
    public static List<String> parseSizes(String sizes) {
        if (sizes == null || sizes.isBlank()) return List.of();
        return java.util.Arrays.stream(sizes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Parse chuỗi "S:10,M:15" thành map, đảm bảo đủ các size của sản phẩm */
    public static Map<String, Integer> parseSizeStocks(String sizeStocksStr, String productSizes, int totalStock) {
        Map<String, Integer> map = new LinkedHashMap<>();
        List<String> expectedSizes = parseSizes(productSizes);

        if (sizeStocksStr != null && !sizeStocksStr.isBlank()) {
            String[] pairs = sizeStocksStr.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    try {
                        map.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        if (!expectedSizes.isEmpty()) {
            for (String s : expectedSizes) {
                map.putIfAbsent(s, 0);
            }
        } else if (map.isEmpty()) {
            map.put("Tiêu chuẩn", totalStock);
        }
        return map;
    }

    public static String toSizeStocksString(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return null;
        return map.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    /**
     * Cộng thêm một lượng vào một size, trả về chuỗi sizeStocks mới.
     * Nếu sizeStocks hiện tại rỗng/null thì lượng CŨ được chia đều cho các size
     * (giữ nguyên cách chia đều như khi khởi tạo kho).
     *
     * @param currentSizeStocks chuỗi "S:10,M:15" hiện tại (có thể null)
     * @param productSizes      chuỗi "S,M,L" các size của sản phẩm
     * @param currentStock      tổng tồn hiện tại (dùng khi chia đều)
     * @param size              size cần cộng (null với sản phẩm không phân size)
     * @param quantity          lượng cần cộng (> 0)
     */
    public static String addToSize(String currentSizeStocks, String productSizes,
                                   int currentStock, String size, int quantity) {
        if (quantity <= 0) return currentSizeStocks;
        Map<String, Integer> map;
        if (currentSizeStocks == null || currentSizeStocks.isBlank()) {
            map = splitEvenly(parseSizes(productSizes), currentStock);
        } else {
            map = parseSizeStocks(currentSizeStocks, productSizes, currentStock);
        }
        String key = (size == null || size.isBlank()) ? defaultKey(map, productSizes) : size;
        map.merge(key, quantity, Integer::sum);
        return toSizeStocksString(map);
    }

    /**
     * Trừ đi một lượng ở một size (dùng khi hủy phiếu nhập đã xác nhận).
     *
     * @throws IllegalArgumentException nếu size không có trong kho hoặc lượng trừ vượt tồn size
     */
    public static String subtractFromSize(String currentSizeStocks, String productSizes,
                                          int currentStock, String size, int quantity) {
        if (quantity <= 0) return currentSizeStocks;
        Map<String, Integer> map = parseSizeStocks(currentSizeStocks, productSizes, currentStock);
        String key = (size == null || size.isBlank()) ? defaultKey(map, productSizes) : size;
        Integer cur = map.get(key);
        if (cur == null || cur < quantity) {
            throw new IllegalArgumentException(
                    "Tồn kho theo size \"" + key + "\" không đủ để hủy lượng đã nhập.");
        }
        map.put(key, cur - quantity);
        return toSizeStocksString(map);
    }

    /** Kho mặc định: chia đều totalStock cho các size (thiếu thì size cuối chịu phần dư) */
    public static Map<String, Integer> splitEvenly(List<String> sizes, int totalStock) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (sizes.isEmpty()) {
            map.put("Tiêu chuẩn", Math.max(0, totalStock));
            return map;
        }
        int per = Math.max(0, totalStock) / sizes.size();
        int remainder = Math.max(0, totalStock) % sizes.size();
        for (int i = 0; i < sizes.size(); i++) {
            map.put(sizes.get(i), per + (i == sizes.size() - 1 ? remainder : 0));
        }
        return map;
    }

    private static String defaultKey(Map<String, Integer> map, String productSizes) {
        if (map.containsKey("Tiêu chuẩn")) return "Tiêu chuẩn";
        List<String> sizes = parseSizes(productSizes);
        if (!sizes.isEmpty()) return sizes.get(0);
        return map.isEmpty() ? "Tiêu chuẩn" : map.keySet().iterator().next();
    }
}
