package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.model.SearchHistory;
import com.storechain.group4.repository.SearchHistoryRepository;
import com.storechain.group4.security.services.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lịch sử tìm kiếm sản phẩm của người dùng đang đăng nhập.
 * Người dùng (kể cả Manager) đều có quyền của User nên dùng được API này.
 */
@RestController
@RequestMapping("/api/user/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private static final int MAX_ITEMS = 10;

    private final SearchHistoryRepository searchHistoryRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistory(
            @RequestParam(required = false) Long shopId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Pageable limit = PageRequest.of(0, MAX_ITEMS);
        List<SearchHistory> items = (shopId != null && shopId > 0)
                ? searchHistoryRepository.findByUserIdAndShopIdOrderBySearchedAtDesc(
                        currentUser.getId(), shopId, limit)
                : searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(currentUser.getId(), limit);
        List<Map<String, Object>> data = items.stream().map(h -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", h.getId());
            m.put("keyword", h.getKeyword());
            m.put("shopId", h.getShopId());
            m.put("searchedAt", h.getSearchedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử tìm kiếm thành công", data));
    }

    /** Ghi nhận một từ khóa tìm kiếm (tự gộp trùng, tối đa 10 mục gần nhất) */
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> addHistory(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        String keyword = body.get("keyword") == null ? "" : String.valueOf(body.get("keyword")).trim();
        Long shopId = null;
        Object sid = body.get("shopId");
        if (sid instanceof Number n && n.longValue() > 0) {
            shopId = n.longValue();
        }
        if (keyword.isEmpty() || keyword.length() > 150) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Từ khóa tìm kiếm không hợp lệ."));
        }
        final Long finalShopId = shopId;
        // Gộp từ khóa trùng: cập nhật thời gian thay vì thêm bản ghi mới
        searchHistoryRepository.findByUserIdAndKeywordIgnoreCase(currentUser.getId(), keyword)
                .ifPresentOrElse(old -> {
                    old.setSearchedAt(LocalDateTime.now());
                    searchHistoryRepository.save(old);
                }, () -> searchHistoryRepository.save(SearchHistory.builder()
                        .user(com.storechain.group4.model.User.builder()
                                .id(currentUser.getId()).build())
                        .shopId(finalShopId)
                        .keyword(keyword)
                        .build()));
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận lịch sử tìm kiếm", keyword));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteOne(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        searchHistoryRepository.findById(id).ifPresent(h -> {
            if (h.getUser().getId().equals(currentUser.getId())) {
                searchHistoryRepository.delete(h);
            }
        });
        return ResponseEntity.ok(ApiResponse.success("Đã xóa mục lịch sử", null));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> clearAll(
            @RequestParam(required = false) Long shopId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (shopId != null && shopId > 0) {
            searchHistoryRepository.deleteByUserIdAndShopId(currentUser.getId(), shopId);
        } else {
            searchHistoryRepository.deleteByUserId(currentUser.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ lịch sử tìm kiếm", null));
    }
}
