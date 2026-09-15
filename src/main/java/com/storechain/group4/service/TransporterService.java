package com.storechain.group4.service;

import com.storechain.group4.dto.TransporterRequestDto;
import com.storechain.group4.dto.TransporterResponseDto;
import com.storechain.group4.dto.TransporterStatsDto;
import org.springframework.data.domain.Page;

public interface TransporterService {

    Page<TransporterResponseDto> getTransporters(
            String keyword,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    TransporterResponseDto getTransporterById(Long id);

    TransporterResponseDto createTransporter(TransporterRequestDto request);

    TransporterResponseDto updateTransporter(Long id, TransporterRequestDto request);

    TransporterResponseDto updateStatus(Long id, String status);

    void deleteTransporter(Long id);

    TransporterStatsDto getStats();
}
