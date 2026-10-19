package com.storechain.group4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private java.math.BigDecimal productPrice;
    private String productSlug;
    private boolean inStock;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
