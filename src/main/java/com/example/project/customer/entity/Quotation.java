package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "rfq_quotations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Integer quoteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    @JsonIgnore
    private Rfq rfq;

    @Column(name = "vendor_id", nullable = false)
    private Integer vendorId;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_lead_time_days")
    private Integer deliveryLeadTimeDays;

    @Column(name = "payment_terms_offered")
    private String paymentTermsOffered;

    @Column(name = "mtc_included")
    @Builder.Default
    @JsonProperty("mtcIncluded")
    private boolean mtcIncluded = true;

    @Column(name = "freight_included")
    @Builder.Default
    @JsonProperty("freightIncluded")
    private boolean freightIncluded = true;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "vendor_rating")
    @Builder.Default
    private Double vendorRating = 4.8;

    @Column(name = "status")
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
