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
public class InvoiceSummaryResponse {

    @JsonProperty("invoiceNumber")
    private String invoiceNumber;

    @JsonProperty("orderId")
    private Integer orderId;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("date")
    private String date;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("downloadUrl")
    private String downloadUrl;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
