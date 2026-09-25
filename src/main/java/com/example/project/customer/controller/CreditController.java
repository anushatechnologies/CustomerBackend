package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CreditApplicationResponse;
import com.example.project.customer.dto.CreditApplyRequest;
import com.example.project.customer.dto.CreditLedgerResponse;
import com.example.project.customer.dto.WalletInfoResponse;
import com.example.project.customer.dto.WalletTransactionResponse;
import com.example.project.customer.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CreditController {

    private final UserContextUtil userContextUtil;
    private final WalletService walletService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CreditApplicationResponse>> applyCredit(
            @RequestBody(required = false) CreditApplyRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        log.info("Processing business credit line application for customer #{}", userId);

        BigDecimal requested = (request != null && request.getRequestedLimit() != null)
                ? request.getRequestedLimit()
                : BigDecimal.valueOf(500000.00);

        Integer tenure = (request != null && request.getTenureDays() != null)
                ? request.getTenureDays()
                : 30;

        String appId = "CR-APP-" + userId + "-" + (System.currentTimeMillis() % 100000);

        CreditApplicationResponse response = CreditApplicationResponse.builder()
                .success(true)
                .applicationId(appId)
                .businessName(request != null ? request.getBusinessName() : "Enterprise Partner")
                .gstin(request != null ? request.getGstin() : null)
                .requestedLimit(requested)
                .approvedLimit(requested)
                .tenureDays(tenure)
                .status("UNDER_REVIEW")
                .message("Credit line application submitted successfully. Verification takes 24-48 business hours.")
                .appliedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Credit application submitted successfully", response));
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<CreditLedgerResponse>> getCreditLedger() {
        Integer userId = userContextUtil.getCurrentUserId();
        log.info("Fetching credit ledger statement for customer #{}", userId);

        BigDecimal creditLimit = BigDecimal.valueOf(500000.00);
        BigDecimal balance = BigDecimal.ZERO;

        try {
            WalletInfoResponse wallet = walletService.getWalletInfo(userId);
            if (wallet != null && wallet.getBalance() != null) {
                balance = wallet.getBalance();
            }
        } catch (Exception ex) {
            log.debug("Wallet lookup non-critical for credit ledger: {}", ex.getMessage());
        }

        BigDecimal availableLimit = creditLimit.add(balance);
        BigDecimal utilizedLimit = BigDecimal.ZERO;
        if (availableLimit.compareTo(creditLimit) < 0) {
            utilizedLimit = creditLimit.subtract(availableLimit);
        }

        List<CreditLedgerResponse.CreditTransactionItem> items = new ArrayList<>();
        try {
            ApiResponse<List<WalletTransactionResponse>> txRes = walletService.getTransactions(userId, 1, 10);
            if (txRes != null && txRes.getData() != null) {
                for (WalletTransactionResponse tx : txRes.getData()) {
                    items.add(CreditLedgerResponse.CreditTransactionItem.builder()
                            .transactionId("CR-TX-" + tx.getId())
                            .type("CREDIT".equalsIgnoreCase(tx.getType()) ? "DRAWDOWN" : "REPAYMENT")
                            .amount(tx.getAmount())
                            .description(tx.getDescription())
                            .referenceNumber(tx.getReferenceId())
                            .date(tx.getTimestamp() != null ? tx.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "")
                            .build());
                }
            }
        } catch (Exception ex) {
            log.debug("Transactions lookup non-critical: {}", ex.getMessage());
        }

        CreditLedgerResponse response = CreditLedgerResponse.builder()
                .creditLimit(creditLimit)
                .availableLimit(availableLimit)
                .utilizedLimit(utilizedLimit)
                .dueAmount(utilizedLimit)
                .dueDate(LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .status("ACTIVE")
                .currency("INR")
                .transactions(items)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Credit ledger retrieved successfully", response));
    }
}
