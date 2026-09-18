package com.example.project.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_usages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouponUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coupon_id", nullable = false) private Coupon coupon;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false) private Order order;
    @Column(name = "discount_applied", nullable = false, precision = 10, scale = 2) private BigDecimal discountApplied;
    @Column(name = "used_at", nullable = false) private LocalDateTime usedAt;
    @PrePersist void create() { if (usedAt == null) usedAt = LocalDateTime.now(); }
}
