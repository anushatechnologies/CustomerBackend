package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditApplyRequest {

    @JsonProperty("businessName")
    private String businessName;

    @JsonProperty("gstin")
    private String gstin;

    @JsonProperty("panNumber")
    private String panNumber;

    @JsonProperty("requestedLimit")
    private BigDecimal requestedLimit;

    @JsonProperty("tenureDays")
    private Integer tenureDays;

    @JsonProperty("annualTurnover")
    private BigDecimal annualTurnover;

    @JsonProperty("financialDocUrls")
    private List<String> financialDocUrls;

    @JsonProperty("notes")
    private String notes;
}
