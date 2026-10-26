package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import com.storechain.group4.dto.TransporterRequestDto;
import com.storechain.group4.dto.TransporterResponseDto;
import com.storechain.group4.dto.TransporterStatsDto;
import com.storechain.group4.service.TransporterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/transporters")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AdminTransporterController {

    private final TransporterService transporterService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransporterResponseDto>>> getTransporters(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<TransporterResponseDto> transporters = transporterService.getTransporters(
                keyword, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn vị vận chuyển thành công", transporters));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TransporterStatsDto>> getTransporterStats() {
        TransporterStatsDto stats = transporterService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu thống kê đơn vị vận chuyển thành công", stats));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransporterResponseDto>> getTransporterById(@PathVariable Long id) {
        try {
            TransporterResponseDto transporter = transporterService.getTransporterById(id);
            return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn vị vận chuyển thành công", transporter));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransporterResponseDto>> createTransporter(
            @Valid @RequestBody TransporterRequestDto request
    ) {
        try {
            TransporterResponseDto created = transporterService.createTransporter(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Thêm đơn vị vận chuyển mới thành công!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransporterResponseDto>> updateTransporter(
            @PathVariable Long id,
            @Valid @RequestBody TransporterRequestDto request
    ) {
        try {
            TransporterResponseDto updated = transporterService.updateTransporter(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật đơn vị vận chuyển thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TransporterResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        try {
            TransporterResponseDto updated = transporterService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn vị vận chuyển thành công!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTransporter(@PathVariable Long id) {
        try {
            transporterService.deleteTransporter(id);
            return ResponseEntity.ok(ApiResponse.success(
                    "Xóa đơn vị vận chuyển thành công!",
                    "Đã xóa đơn vị vận chuyển ID: " + id
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
