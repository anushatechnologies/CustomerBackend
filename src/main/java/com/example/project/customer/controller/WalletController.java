package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.RewardVoucherResponse;
import com.example.project.customer.dto.WalletInfoResponse;
import com.example.project.customer.dto.WalletTopupRequest;
import com.example.project.customer.dto.WalletTransactionResponse;
import com.example.project.customer.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserContextUtil userContextUtil;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<WalletInfoResponse>> getWalletInfo() {
        Integer userId = userContextUtil.getCurrentUserId();
        WalletInfoResponse info = walletService.getWalletInfo(userId);
        return ResponseEntity.ok(ApiResponse.ok("Wallet information retrieved successfully", info));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getTransactions(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        Integer userId = userContextUtil.getCurrentUserId();
        ApiResponse<List<WalletTransactionResponse>> response = walletService.getTransactions(userId, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<RewardVoucherResponse>>> getRewards() {
        Integer userId = userContextUtil.getCurrentUserId();
        List<RewardVoucherResponse> rewards = walletService.getRewards(userId);
        return ResponseEntity.ok(ApiResponse.ok("Active reward vouchers retrieved successfully", rewards));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<WalletInfoResponse>> topup(@Valid @RequestBody WalletTopupRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        WalletInfoResponse updated = walletService.topup(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Wallet topped up successfully", updated));
    }
}
