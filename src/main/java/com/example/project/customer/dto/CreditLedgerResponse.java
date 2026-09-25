package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditLedgerResponse {

    @JsonProperty("creditLimit")
    private BigDecimal creditLimit;

    @JsonProperty("availableLimit")
    private BigDecimal availableLimit;

    @JsonProperty("utilizedLimit")
    private BigDecimal utilizedLimit;

    @JsonProperty("dueAmount")
    private BigDecimal dueAmount;

    @JsonProperty("dueDate")
    private String dueDate;

    @JsonProperty("status")
    private String status;

    @JsonProperty("currency")
    @Builder.Default
    private String currency = "INR";

    @JsonProperty("transactions")
    private List<CreditTransactionItem> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreditTransactionItem {
        private String transactionId;
        private String type; // DRAWDOWN or REPAYMENT
        private BigDecimal amount;
        private String description;
        private String referenceNumber;
        private String date;
    }
}
