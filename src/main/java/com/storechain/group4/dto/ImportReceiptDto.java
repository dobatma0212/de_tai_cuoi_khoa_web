package com.storechain.group4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Phiếu nhập hàng trả về cho manager */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReceiptDto {

    private Long id;
    private String receiptCode;
    private Long shopId;
    private String shopName;
    /** DRAFT / CONFIRMED / CANCELLED */
    private String status;
    private String sourceNote;
    private String note;
    private int totalQuantity;
    private Long createdById;
    private String createdByUsername;
    private String createdByFullName;
    private String confirmedByUsername;
    private String confirmedByFullName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    private List<ImportItemDto> items;
}
