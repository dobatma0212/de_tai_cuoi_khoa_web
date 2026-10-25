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
 * Geocoding API thật (phương án 1 của nhóm trưởng): gọi Photon (OpenStreetMap,
 * miễn phí, không cần key) để chuyển một chuỗi địa chỉ Việt Nam thành tọa độ
 * {vĩ độ, kinh độ}. Kết quả được cache trong bộ nhớ (địa chỉ chi nhánh/địa chỉ
 * khách lặp lại nhiều lần) để không spam API và giữ checkout nhanh.
 *
 * Có hai lớp tự vệ:
 * 1. API lỗi/mạng chậm thì trả null — caller fallback về VietnamGeoService
 *    (bảng tọa độ tâm tỉnh nhúng sẵn).
 * 2. geocodeWithFallback đối chiếu TÊN QUẬN của kết quả với quận khách khai
 *    báo: địa chỉ vô lý (phường không tồn tại ở quận đó) có thể khiến API ghép
 *    nhầm sang nơi khác — lúc đó dùng tọa độ của quận khai báo thay vì điểm sai.
 */
@Service
@Slf4j
public class GeocodingService {

    private static final String PHOTON_URL = "https://photon.komoot.io/api/?limit=1&q=";
    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    /** Kết quả geocode: tọa độ + tên quận/huyện theo dữ liệu của API */
    public record GeoPoint(double lat, double lng, String district) {
        public double[] coords() { return new double[]{lat, lng}; }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Cache địa chỉ đã geocode; FAILED = đã thử và thất bại (chống retry liên tục) */
    private static final GeoPoint FAILED = new GeoPoint(0, 0, null);
    private final Map<String, GeoPoint> cache = new ConcurrentHashMap<>();

    /**
     * Geocode một địa chỉ (VD "Số 1 Đường 1, TP. Dĩ An, Bình Dương").
     *
     * @return {lat, lng} hoặc null nếu không tìm được / API lỗi
     */
    public double[] geocode(String address) {
        GeoPoint p = geocodeDetailed(address);
        return p == null ? null : p.coords();
    }

    /** Geocode kèm tên quận/huyện của kết quả (để đối chiếu với địa chỉ khai báo) */
    public GeoPoint geocodeDetailed(String address) {
        if (address == null || address.isBlank()) return null;
        String key = address.trim().toLowerCase(java.util.Locale.ROOT);
        if (cache.containsKey(key)) {
            GeoPoint hit = cache.get(key);
            return hit == FAILED ? null : hit;
        }
        GeoPoint result = callPhoton(address.trim());
        cache.put(key, result != null ? result : FAILED);
        if (result != null) {
            log.info("Geocoded \"{}\" -> {}, {} ({})", address, result.lat(), result.lng(), result.district());
        } else {
            log.info("Geocode thất bại cho \"{}\"", address);
        }
        return result;
    }

    /**
     * Geocode có kiểm tra hợp lệ với bias theo tỉnh:
     * 1. Điểm tham chiếu = geocode("quận + tỉnh") CÓ BIAS tọa độ tâm tỉnh
     *    (không bias thì Photon hay trả quận trùng tên của tỉnh khác).
     * 2. Geocode chuỗi đầy đủ; chỉ chấp nhận nếu nằm trong ~10km quanh điểm
     *    tham chiếu. Địa chỉ vô lý (phường không thuộc quận đó, đường fake)
     *    thường khiến API ghép ra nơi khác — lúc đó dùng điểm quận.
     *
     * @param bias tọa độ tâm tỉnh {lat, lng} (từ VietnamGeoService), có thể null
     */
    public double[] geocodeWithFallback(String fullAddress, double[] bias) {
        if (fullAddress == null || fullAddress.isBlank()) return null;
        String[] parts = fullAddress.split(",");
        GeoPoint districtRef = null;
        if (parts.length >= 2) {
            districtRef = geocodeBiased(parts[parts.length - 2].trim() + ", "
                    + parts[parts.length - 1].trim(), bias);
        }
        if (districtRef == null && parts.length >= 1) {
            districtRef = geocodeBiased(parts[parts.length - 1].trim(), bias);
        }
        GeoPoint full = geocodeDetailed(fullAddress);
        if (full == null) return districtRef == null ? null : districtRef.coords();
        if (districtRef == null) return full.coords();
        Double driftKm = haversineKm(full.coords(), districtRef.coords());
        // Lệch quá 10km khỏi quận khai báo -> API đã ghép nhầm nơi khác
        if (driftKm != null && driftKm > 10) {
            log.info("Geocode bỏ điểm lệch {}km khỏi quận \"{}\" -> dùng tọa độ quận",
                    String.format("%.1f", driftKm), parts[parts.length - 2].trim());
            return districtRef.coords();
        }
        return full.coords();
    }

    /** Geocode có bias: ưu tiên kết quả gần tọa độ tỉnh đã biết */
    private GeoPoint geocodeBiased(String query, double[] bias) {
        if (bias == null) return geocodeDetailed(query);
        try {
            String url = PHOTON_URL + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&lat=" + bias[0] + "&lon=" + bias[1];
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "StoreChainWebApp/1.0 (student-project)")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode features = root.path("features");
                if (features.isArray() && !features.isEmpty()) {
                    JsonNode feature = features.get(0);
                    if ("VN".equalsIgnoreCase(feature.path("properties").path("countrycode").asText())) {
                        JsonNode coords = feature.path("geometry").path("coordinates");
                        if (coords.isArray() && coords.size() >= 2) {
                            double lng = coords.get(0).asDouble();
                            double lat = coords.get(1).asDouble();
                            if (lat != 0 || lng != 0) {
                                String district = feature.path("properties").path("district").asText(null);
                                if (district == null) district = feature.path("properties").path("county").asText(null);
                                GeoPoint point = new GeoPoint(lat, lng, district);
                                cache.put(query.trim().toLowerCase(java.util.Locale.ROOT), point);
                                log.info("Geocoded (bias) \"{}\" -> {}, {} ({})", query, lat, lng, district);
                                return point;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi gọi Photon (bias): {}", e.getMessage());
        }
        return geocodeDetailed(query);
    }

    private static Double haversineKm(double[] a, double[] b) {
        if (a == null || b == null) return null;
        final int R = 6371;
        double dLat = Math.toRadians(b[0] - a[0]);
        double dLng = Math.toRadians(b[1] - a[1]);
        double s = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a[0])) * Math.cos(Math.toRadians(b[0]))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s));
    }

    private GeoPoint callPhoton(String address) {
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
            String district = feature.path("properties").path("district").asText(null);
            if (district == null) district = feature.path("properties").path("county").asText(null);
            return new GeoPoint(lat, lng, district);
        } catch (Exception e) {
            log.warn("Lỗi gọi Photon geocoding: {}", e.getMessage());
            return null;
        }
    }
}
