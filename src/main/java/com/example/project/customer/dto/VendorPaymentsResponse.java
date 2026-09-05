package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorPaymentsResponse {
    private Integer sellerId;
    private BigDecimal totalSettledAmount;
    private BigDecimal pendingPayout;
    private BigDecimal upcomingEscrowAmount;
    private LocalDate nextSettlementDate;
    private String bankName;
    private String bankAccountNumberMasked;
    private String ifscCode;
    private List<PayoutRecord> payouts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayoutRecord {
        private String payoutId;
        private LocalDate payoutDate;
        private BigDecimal amount;
        private String status; // SETTLED, PROCESSING, IN_ESCROW
        private String utrNumber;
        private Integer orderCount;
    }
}
