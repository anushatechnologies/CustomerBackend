package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "type", nullable = false)
    private String type; // CREDIT or DEBIT

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "source")
    private String source; // ORDER_CASHBACK, TOPUP, REFUND, PURCHASE_PAYMENT, REWARD

    @Column(name = "reference_id")
    private String referenceId; // e.g. ORD-xxx or TXN-xxx

    @Column(name = "description")
    private String description;

    @Column(name = "balance_after", precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
