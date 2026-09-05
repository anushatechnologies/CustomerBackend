package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "reward_vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId; // null means global / available to all

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "discount_type", nullable = false)
    private String discountType; // PERCENTAGE, FIXED

    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_redeemed")
    @Builder.Default
    private Boolean redeemed = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
