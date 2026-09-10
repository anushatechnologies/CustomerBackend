package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.RewardVoucherResponse;
import com.example.project.customer.dto.WalletInfoResponse;
import com.example.project.customer.dto.WalletTopupRequest;
import com.example.project.customer.dto.WalletTransactionResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.RewardVoucher;
import com.example.project.customer.entity.Wallet;
import com.example.project.customer.entity.WalletTransaction;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.repository.RewardVoucherRepository;
import com.example.project.customer.repository.WalletRepository;
import com.example.project.customer.repository.WalletTransactionRepository;
import com.example.project.customer.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final RewardVoucherRepository rewardVoucherRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletInfoResponse getWalletInfo(Integer userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return mapToWalletInfo(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<WalletTransactionResponse>> getTransactions(Integer userId, int page, int limit) {
        int pageNumber = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageNumber, limit);
        Page<WalletTransaction> paged = transactionRepository.findByWallet_Customer_CustomerIdOrderByTimestampDesc(userId, pageable);

        List<WalletTransactionResponse> data = paged.getContent().stream()
                .map(this::mapToTransactionResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(page, limit, paged.getTotalElements());
        return ApiResponse.paginated("Wallet transactions retrieved successfully", data, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardVoucherResponse> getRewards(Integer userId) {
        List<RewardVoucher> vouchers = rewardVoucherRepository.findAvailableForUser(userId);
        return vouchers.stream()
                .map(this::mapToRewardResponse)
                .toList();
    }

    @Override
    public WalletInfoResponse topup(Integer userId, WalletTopupRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Manual wallet top-up is restricted to administrators. Customers must top up their wallet via Razorpay checkout.");
        }

        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        wallet = walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type("CREDIT")
                .amount(request.getAmount())
                .source("TOPUP")
                .referenceId("TOP-" + System.currentTimeMillis())
                .description(request.getDescription() != null ? request.getDescription() : "Wallet Top-up via Direct Transfer")
                .balanceAfter(newBalance)
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(txn);

        return mapToWalletInfo(wallet);
    }

    private Wallet getOrCreateWallet(Integer userId) {
        return walletRepository.findByCustomer_CustomerId(userId)
                .orElseGet(() -> {
                    Wallet w = Wallet.builder()
                            .customer(Customer.builder().customerId(userId).build())
                            .balance(BigDecimal.ZERO)
                            .currency("INR")
                            .loyaltyPoints(0)
                            .tier("STANDARD")
                            .active(true)
                            .build();
                    return walletRepository.save(w);
                });
    }

    private WalletInfoResponse mapToWalletInfo(Wallet wallet) {
        return WalletInfoResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getCustomer() != null ? wallet.getCustomer().getCustomerId() : null)
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .loyaltyPoints(wallet.getLoyaltyPoints())
                .tier(wallet.getTier())
                .active(wallet.getActive())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction t) {
        return WalletTransactionResponse.builder()
                .id(t.getId())
                .walletId(t.getWallet().getWalletId())
                .type(t.getType())
                .amount(t.getAmount())
                .source(t.getSource())
                .referenceId(t.getReferenceId())
                .description(t.getDescription())
                .balanceAfter(t.getBalanceAfter())
                .timestamp(t.getTimestamp())
                .build();
    }

    private RewardVoucherResponse mapToRewardResponse(RewardVoucher v) {
        return RewardVoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .title(v.getTitle())
                .description(v.getDescription())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .minOrderValue(v.getMinOrderValue())
                .maxDiscount(v.getMaxDiscount())
                .expiryDate(v.getExpiryDate())
                .redeemed(v.getRedeemed())
                .active(v.getActive())
                .build();
    }
}
