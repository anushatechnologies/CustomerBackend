package com.example.project.customer.dto.estimation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProductSummary {
    private Integer productId;
    private String title;
    private String brandName;
    private String categoryName;
    private String subcategoryName;
    private BigDecimal price;
    private BigDecimal mrp;
    private String unit;
    private String imageUrl;
    private Map<String, String> specifications;
    private Integer stockQty;
    private Boolean isAvailable;
}
