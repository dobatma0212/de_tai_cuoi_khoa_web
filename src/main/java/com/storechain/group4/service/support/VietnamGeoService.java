package com.storechain.group4.service.support;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tọa độ tâm của 63 tỉnh/thành Việt Nam (vĩ độ, kinh độ xấp xỉ) để tính khoảng
 * cách thật giữa khách hàng và chi nhánh khi phân phối đơn hàng (haversine).
 * Dữ liệu nhúng sẵn nên không phụ thuộc API ngoài lúc chạy.
 */
@Service
public class VietnamGeoService {

    /** Tỉnh/thành (tên đã chuẩn hóa không dấu) -> {vĩ độ, kinh độ} */
    private static final Map<String, double[]> CENTROIDS = new TreeMap<>();

    static {
        put("ha noi", 21.03, 105.85);
        put("ha giang", 22.68, 104.98);
        put("cao bang", 22.66, 106.26);
        put("bac kan", 22.14, 105.86);
        put("tuyen quang", 21.82, 105.38);
        put("lao cai", 22.49, 103.97);
        put("dien bien", 21.39, 103.02);
        put("lai chau", 22.40, 103.46);
        put("son la", 21.33, 103.92);
        put("yen bai", 21.72, 104.90);
        put("hoa binh", 20.86, 105.34);
        put("thai nguyen", 21.59, 105.85);
        put("lang son", 21.85, 106.76);
        put("quang ninh", 21.01, 107.00);
        put("bac giang", 21.35, 106.19);
        put("phu tho", 21.32, 105.40);
        put("vinh phuc", 21.31, 105.60);
        put("bac ninh", 21.19, 106.08);
        put("hai duong", 20.94, 106.33);
        put("hai phong", 20.86, 106.68);
        put("hung yen", 20.65, 106.05);
        put("thai binh", 20.45, 106.34);
        put("ha nam", 20.59, 106.05);
        put("nam dinh", 20.42, 106.17);
        put("ninh binh", 20.25, 105.97);
        put("thanh hoa", 19.81, 105.78);
        put("nghe an", 18.68, 105.68);
        put("ha tinh", 18.34, 105.90);
        put("quang binh", 17.47, 106.60);
        put("quang tri", 16.75, 107.20);
        put("thua thien hue", 16.46, 107.60);
        put("hue", 16.46, 107.60);
        put("da nang", 16.07, 108.22);
        put("quang nam", 15.54, 107.96);
        put("quang ngai", 15.12, 108.79);
        put("binh dinh", 13.78, 109.22);
        put("phu yen", 13.25, 109.32);
        put("khanh hoa", 12.26, 109.00);
        put("ninh thuan", 11.67, 108.99);
        put("binh thuan", 11.33, 108.47);
        put("kon tum", 14.35, 108.00);
        put("gia lai", 13.98, 108.00);
        put("dak lak", 12.71, 108.24);
        put("dak nog", 12.24, 107.79);
        put("lam dong", 11.94, 108.46);
        put("da lat", 11.94, 108.46);
        put("binh phuoc", 11.75, 106.90);
        put("tay ninh", 11.31, 106.10);
        put("binh duong", 11.20, 106.65);
        put("thu dau mot", 11.20, 106.65);
        put("dong nai", 10.95, 106.85);
        put("bien hoa", 10.95, 106.85);
        put("ho chi minh", 10.78, 106.70);
        put("tp ho chi minh", 10.78, 106.70);
        put("tphcm", 10.78, 106.70);
        put("saigon", 10.78, 106.70);
        put("ba ria vung tau", 10.60, 107.07);
        put("vung tau", 10.60, 107.07);
        put("long an", 10.53, 106.43);
        put("tien giang", 10.36, 106.34);
        put("my tho", 10.36, 106.34);
        put("ben tre", 10.24, 106.38);
        put("tra vinh", 9.95, 106.35);
        put("vinh long", 10.03, 105.97);
        put("dong thap", 10.45, 105.63);
        put("cao lanh", 10.45, 105.63);
        put("an giang", 10.52, 105.08);
        put("long xuyen", 10.52, 105.08);
        put("kien giang", 10.01, 105.08);
        put("rac gia", 10.01, 105.08);
        put("can tho", 10.03, 105.78);
        put("hau giang", 9.91, 105.64);
        put("soc trang", 9.60, 105.98);
        put("bac lieu", 9.28, 105.72);
        put("ca mau", 9.18, 105.15);
    }

    private static void put(String name, double lat, double lng) {
        CENTROIDS.put(name, new double[]{lat, lng});
    }

    /**
     * Tìm tọa độ từ một chuỗi địa chỉ/tỉnh bất kỳ (khớp mềm theo tên đã chuẩn
     * hóa: bỏ dấu, bỏ tiền tố Tỉnh/Thành phố/TP.). VD: "Thành phố Hồ Chí Minh",
     * "Binh Duong", "TPHCM" đều ra tọa độ riêng.
     *
     * @return {lat, lng} hoặc null nếu không nhận diện được tỉnh nào
     */
    public double[] resolve(String provinceOrAddress) {
        if (provinceOrAddress == null || provinceOrAddress.isBlank()) return null;
        String n = normalize(provinceOrAddress);
        if (n.isEmpty()) return null;
        double[] exact = CENTROIDS.get(n);
        if (exact != null) return exact;
        // Khớp chứa: ưu tiên tên tỉnh dài nhất xuất hiện trong chuỗi địa chỉ
        String best = null;
        for (String key : CENTROIDS.keySet()) {
            if ((n.equals(key) || n.contains(key) || key.contains(n))
                    && (best == null || key.length() > best.length())) {
                best = key;
            }
        }
        return best == null ? null : CENTROIDS.get(best);
    }

    /** Khoảng cách haversine giữa hai tọa độ (km); null nếu thiếu tọa độ */
    public Double distanceKm(double[] a, double[] b) {
        if (a == null || b == null) return null;
        final int R = 6371; // bán kính Trái Đất (km)
        double dLat = Math.toRadians(b[0] - a[0]);
        double dLng = Math.toRadians(b[1] - a[1]);
        double s = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a[0])) * Math.cos(Math.toRadians(b[0]))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s));
    }

    /** Chuẩn hóa: bỏ dấu, thường hóa, bỏ tiền tố hành chính */
    private String normalize(String s) {
        String p = s.trim().toLowerCase(Locale.ROOT);
        p = Normalizer.normalize(p, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        p = p.replace('đ', 'd').replace('Đ', 'd');
        p = p.replaceAll("^(tinh|thanh pho|tp|quan|huyen)\\.?\\s*", "");
        return p.replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }
}
