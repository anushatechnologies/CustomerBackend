package com.example.project.customer.service;

import com.example.project.customer.dto.WalletInfoResponse;
import com.example.project.customer.dto.WalletTopupRequest;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Wallet;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.repository.RewardVoucherRepository;
import com.example.project.customer.repository.WalletRepository;
import com.example.project.customer.repository.WalletTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private RewardVoucherRepository rewardVoucherRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getWalletInfo - creates new wallet with zero balance, zero points, and STANDARD tier")
    void testGetWalletInfo_CreatesNewWalletWithZeroBalance() {
        when(walletRepository.findByCustomer_CustomerId(101)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            w.setWalletId(1);
            return w;
        });

        WalletInfoResponse response = walletService.getWalletInfo(101);

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getLoyaltyPoints()).isEqualTo(0);
        assertThat(response.getTier()).isEqualTo("STANDARD");
    }

    @Test
    @DisplayName("topup - throws ForbiddenException when customer attempts manual top-up")
    void testTopup_CustomerForbidden() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "buyer", null, List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        WalletTopupRequest request = WalletTopupRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .build();

        assertThatThrownBy(() -> walletService.topup(101, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("restricted to administrators");
    }

    @Test
    @DisplayName("topup - succeeds when invoked by ADMIN")
    void testTopup_AdminSuccess() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Wallet existingWallet = Wallet.builder()
                .walletId(1)
                .customer(Customer.builder().customerId(101).build())
                .balance(BigDecimal.valueOf(100.00))
                .currency("INR")
                .loyaltyPoints(0)
                .tier("STANDARD")
                .active(true)
                .build();

        when(walletRepository.findByCustomer_CustomerId(101)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTopupRequest request = WalletTopupRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .description("Admin topup")
                .build();

        WalletInfoResponse response = walletService.topup(101, request);

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600.00));
        verify(transactionRepository).save(any());
    }
}
