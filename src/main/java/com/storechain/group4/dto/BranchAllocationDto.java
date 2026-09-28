package com.storechain.group4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchAllocationDto {

    private Long shopId;
    private String shopName;
    private String shopAddress;
    private int totalStock;
    /** Phân bổ số lượng theo từng size: ví dụ {"S": 10, "M": 15, "L": 12, "XL": 8} */
    private Map<String, Integer> sizeStocks;
}
