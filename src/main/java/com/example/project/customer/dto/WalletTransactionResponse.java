package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class WalletTransactionResponse {
    private Integer id;
    private Integer walletId;
    private String type;
    private BigDecimal amount;
    private String source;
    private String referenceId;
    private String description;
    private BigDecimal balanceAfter;
    private LocalDateTime timestamp;
}
