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
public class WalletInfoResponse {
    private Integer walletId;
    private Integer userId;
    private BigDecimal balance;
    private String currency;
    private Integer loyaltyPoints;
    private String tier;
    private Boolean active;
    private LocalDateTime updatedAt;
}
