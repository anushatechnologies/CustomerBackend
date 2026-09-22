package com.example.project.customer.dto.estimation;

import com.example.project.customer.entity.MatchStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstimationItemResponse {
    private Long itemId;
    private String rawItemName;
    private BigDecimal requestedQuantity;
    private String requestedUnit;
    private String requestedBrand;
    private String dimensions;
    private String specifications;
    private String notes;

    private MatchStatus matchStatus;

    private Integer matchedProductId;
    private String matchedProductTitle;
    private String matchedProductSku;
    private String matchedProductImageUrl;
    private String matchedProductUnit;

    private BigDecimal unitPrice;
    private String appliedTierDescription;
    private BigDecimal gstRate;
    private BigDecimal lineSubtotal;
    private BigDecimal lineTax;
    private BigDecimal lineTotal;

    private Boolean isAvailable;
    private Integer availableStock;

    @Builder.Default
    private List<CandidateProductSummary> candidateProducts = new ArrayList<>();
}
