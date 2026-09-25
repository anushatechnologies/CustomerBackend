package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderMtcResponse {

    @JsonProperty("orderId")
    private Integer orderId;

    @JsonProperty("certificateNumber")
    private String certificateNumber;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("heatNumber")
    private String heatNumber;

    @JsonProperty("batchNumber")
    private String batchNumber;

    @JsonProperty("grade")
    private String grade;

    @JsonProperty("inspectionAgency")
    private String inspectionAgency;

    @JsonProperty("chemicalAnalysis")
    private Map<String, String> chemicalAnalysis;

    @JsonProperty("mechanicalProperties")
    private Map<String, String> mechanicalProperties;

    @JsonProperty("status")
    private String status;

    @JsonProperty("verified")
    @Builder.Default
    private boolean verified = true;

    @JsonProperty("downloadUrl")
    private String downloadUrl;

    @JsonProperty("issuedAt")
    private LocalDateTime issuedAt;
}
