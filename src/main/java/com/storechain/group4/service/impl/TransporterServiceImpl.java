package com.storechain.group4.service.impl;

import com.storechain.group4.dto.TransporterRequestDto;
import com.storechain.group4.dto.TransporterResponseDto;
import com.storechain.group4.dto.TransporterStatsDto;
import com.storechain.group4.model.Transporter;
import com.storechain.group4.repository.TransporterRepository;
import com.storechain.group4.service.TransporterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransporterServiceImpl implements TransporterService {

    private final TransporterRepository transporterRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "phone", "email", "baseFee", "status", "createdAt"
    );

    @Override
    @Transactional(readOnly = true)
    public Page<TransporterResponseDto> getTransporters(
            String keyword,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        if (sortBy == null || sortBy.isBlank() || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "id";
        }

        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return transporterRepository.searchTransporters(keyword, status, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TransporterResponseDto getTransporterById(Long id) {
        Transporter transporter = transporterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển với ID: " + id));
        return toDto(transporter);
    }

    @Override
    @Transactional
    public TransporterResponseDto createTransporter(TransporterRequestDto request) {
        String name = normalizeRequired(request.getName(), "Tên đơn vị vận chuyển không được để trống");
        String phone = normalizeRequired(request.getPhone(), "Số điện thoại không được để trống");
        String email = normalizeOptionalEmail(request.getEmail());

        if (email != null && transporterRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng bởi đơn vị vận chuyển khác");
        }

        BigDecimal baseFee = request.getBaseFee() != null ? request.getBaseFee() : BigDecimal.ZERO;
        if (baseFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí cơ bản không được âm");
        }

        Transporter transporter = Transporter.builder()
                .name(name)
                .phone(phone)
                .email(email)
                .baseFee(baseFee)
                .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus() : "active")
                .build();

        Transporter saved = transporterRepository.save(transporter);
        log.info("Đã tạo đơn vị vận chuyển mới: ID={}, Name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public TransporterResponseDto updateTransporter(Long id, TransporterRequestDto request) {
        Transporter transporter = transporterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển với ID: " + id));

        String name = normalizeRequired(request.getName(), "Tên đơn vị vận chuyển không được để trống");
        String phone = normalizeRequired(request.getPhone(), "Số điện thoại không được để trống");
        String email = normalizeOptionalEmail(request.getEmail());

        if (email != null && transporterRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng bởi đơn vị vận chuyển khác");
        }

        BigDecimal baseFee = request.getBaseFee() != null ? request.getBaseFee() : BigDecimal.ZERO;
        if (baseFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí cơ bản không được âm");
        }

        transporter.setName(name);
        transporter.setPhone(phone);
        transporter.setEmail(email);
        transporter.setBaseFee(baseFee);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            transporter.setStatus(request.getStatus());
        }

        Transporter updated = transporterRepository.save(transporter);
        log.info("Đã cập nhật đơn vị vận chuyển: ID={}, Name={}", updated.getId(), updated.getName());
        return toDto(updated);
    }

    @Override
    @Transactional
    public TransporterResponseDto updateStatus(Long id, String status) {
        if (!"active".equalsIgnoreCase(status) && !"inactive".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'active' hoặc 'inactive'");
        }
        Transporter transporter = transporterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển với ID: " + id));

        transporter.setStatus(status.toLowerCase());
        Transporter saved = transporterRepository.save(transporter);
        log.info("Đã cập nhật trạng thái đơn vị vận chuyển ID {} thành {}", id, status);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteTransporter(Long id) {
        Transporter transporter = transporterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị vận chuyển với ID: " + id));

        transporterRepository.delete(transporter);
        log.info("Đã xóa đơn vị vận chuyển ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TransporterStatsDto getStats() {
        long total = transporterRepository.count();
        long active = transporterRepository.countByStatus("active");
        long inactive = transporterRepository.countByStatus("inactive");
        BigDecimal avg = transporterRepository.averageBaseFee();
        if (avg == null) {
            avg = BigDecimal.ZERO;
        } else {
            avg = avg.setScale(2, RoundingMode.HALF_UP);
        }

        return TransporterStatsDto.builder()
                .totalTransporters(total)
                .activeTransporters(active)
                .inactiveTransporters(inactive)
                .avgBaseFee(avg)
                .build();
    }

    private TransporterResponseDto toDto(Transporter transporter) {
        return TransporterResponseDto.builder()
                .id(transporter.getId())
                .name(transporter.getName())
                .phone(transporter.getPhone())
                .email(transporter.getEmail())
                .baseFee(transporter.getBaseFee())
                .status(transporter.getStatus())
                .createdAt(transporter.getCreatedAt())
                .build();
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = value != null ? value.trim() : "";
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
