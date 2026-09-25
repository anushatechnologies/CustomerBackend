package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditApplicationResponse {

    @Builder.Default
    @JsonProperty("success")
    private boolean success = true;

    @JsonProperty("applicationId")
    private String applicationId;

    @JsonProperty("businessName")
    private String businessName;

    @JsonProperty("gstin")
    private String gstin;

    @JsonProperty("requestedLimit")
    private BigDecimal requestedLimit;

    @JsonProperty("approvedLimit")
    private BigDecimal approvedLimit;

    @JsonProperty("tenureDays")
    private Integer tenureDays;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("appliedAt")
    private LocalDateTime appliedAt;
}
