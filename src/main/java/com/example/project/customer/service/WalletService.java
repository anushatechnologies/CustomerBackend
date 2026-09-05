package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.RewardVoucherResponse;
import com.example.project.customer.dto.WalletInfoResponse;
import com.example.project.customer.dto.WalletTopupRequest;
import com.example.project.customer.dto.WalletTransactionResponse;

import java.util.List;

public interface WalletService {
    WalletInfoResponse getWalletInfo(Integer userId);
    ApiResponse<List<WalletTransactionResponse>> getTransactions(Integer userId, int page, int limit);
    List<RewardVoucherResponse> getRewards(Integer userId);
    WalletInfoResponse topup(Integer userId, WalletTopupRequest request);
}
