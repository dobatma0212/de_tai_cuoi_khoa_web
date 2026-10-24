package com.storechain.group4.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phương án 1 của nhóm trưởng: Geocoding API thật.
 * Gọi Photon (OpenStreetMap-based, miễn phí, không cần key) để chuyển một
 * chuỗi địa chỉ Việt Nam thành tọa độ {vĩ độ, kinh độ}. Kết quả được cache
 * trong bộ nhớ (địa chỉ chi nhánh/địa chỉ khách lặp lại nhiều lần) để không
 * spam API và giữ checkout nhanh. API lỗi/mạng chậm thì trả null — caller
 * fallback về VietnamGeoService (bảng tọa độ tâm tỉnh nhúng sẵn).
 */
@Service
@Slf4j
public class GeocodingService {

    private static final String PHOTON_URL = "https://photon.komoot.io/api/?limit=1&q=";
    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Cache địa chỉ đã geocode -> {lat, lng}; mảng rỗng = đã thử và thất bại (chống retry liên tục) */
    private static final double[] FAILED = new double[0];
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    /**
     * Geocode một địa chỉ (VD "Số 1 Đường 1, TP. Dĩ An, Bình Dương").
     *
     * @return {lat, lng} hoặc null nếu không tìm được / API lỗi
     */
    public double[] geocode(String address) {
        if (address == null || address.isBlank()) return null;
        String key = address.trim().toLowerCase(java.util.Locale.ROOT);
        if (cache.containsKey(key)) {
            double[] hit = cache.get(key);
            return hit.length == 0 ? null : hit;
        }
        double[] result = callPhoton(address.trim());
        cache.put(key, result != null ? result : FAILED);
        if (result != null) {
            log.info("Geocoded \"{}\" -> {}, {}", address, result[0], result[1]);
        } else {
            log.info("Geocode thất bại cho \"{}\" (dùng fallback tọa độ tỉnh)", address);
        }
        return result;
    }

    private double[] callPhoton(String address) {
        try {
            String url = PHOTON_URL + URLEncoder.encode(address, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "StoreChainWebApp/1.0 (student-project)")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            JsonNode root = mapper.readTree(response.body());
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) return null;
            JsonNode feature = features.get(0);
            // Chỉ nhận kết quả thuộc Việt Nam
            if (!"VN".equalsIgnoreCase(feature.path("properties").path("countrycode").asText())) return null;
            JsonNode coords = feature.path("geometry").path("coordinates");
            if (!coords.isArray() || coords.size() < 2) return null;
            // GeoJSON: [longitude, latitude]
            double lng = coords.get(0).asDouble();
            double lat = coords.get(1).asDouble();
            if (lat == 0 || lng == 0) return null;
            return new double[]{lat, lng};
        } catch (Exception e) {
            log.warn("Lỗi gọi Photon geocoding: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Geocode với chuỗi truy vấn ưu tiên: thử chuỗi đầy đủ trước; nếu thất bại
     * thì thử phần tỉnh/thành (cuối chuỗi) để vẫn ra tọa độ tâm tỉnh.
     */
    public double[] geocodeWithFallback(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) return null;
        double[] full = geocode(fullAddress);
        if (full != null) return full;
        // thử các phần sau dấu phẩy (tỉnh thường là phần cuối)
        String[] parts = fullAddress.split(",");
        for (int i = Math.max(0, parts.length - 3); i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.length() < 3) continue;
            double[] r = geocode(part);
            if (r != null) return r;
        }
        return null;
    }
}
