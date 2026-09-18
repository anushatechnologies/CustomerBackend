package com.example.project.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 150) private String title;
    @Column(length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false) private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2) private BigDecimal discountValue;
    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal minOrderAmount = BigDecimal.ZERO;
    @Column(name = "max_discount_amount", precision = 10, scale = 2) private BigDecimal maxDiscountAmount;
    @Column(name = "start_date") private LocalDateTime startDate;
    @Column(name = "expiry_date") private LocalDateTime expiryDate;
    @Column(name = "total_usage_limit") private Integer totalUsageLimit;
    @Column(name = "used_count", nullable = false) @Builder.Default private Integer usedCount = 0;
    @Column(name = "per_user_limit", nullable = false) @Builder.Default private Integer perUserLimit = 1;
    @Column(name = "first_order_only", nullable = false) @Builder.Default private Boolean firstOrderOnly = false;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void create() { LocalDateTime now = LocalDateTime.now(); createdAt = createdAt == null ? now : createdAt; updatedAt = now; code = code == null ? null : code.trim().toUpperCase(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); code = code == null ? null : code.trim().toUpperCase(); }
}
