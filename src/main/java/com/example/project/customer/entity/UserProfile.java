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
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private String role = "BUYER";

    @Column(nullable = false)
    @Builder.Default
    private String tier = "GOLD";

    @Column(name = "is_profile_complete")
    @Builder.Default
    private boolean profileComplete = true;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "is_gst_verified")
    @Builder.Default
    private boolean gstVerified = true;

    @Column(name = "credit_limit", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.valueOf(5000000.0);

    @Column(name = "available_credit", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal availableCredit = BigDecimal.valueOf(3250000.0);

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
