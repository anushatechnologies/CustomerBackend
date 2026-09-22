package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "estimation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estimation_id", nullable = false)
    @JsonIgnore
    private Estimation estimation;

    @Column(name = "raw_item_name", nullable = false)
    private String rawItemName;

    @Column(name = "requested_quantity", precision = 12, scale = 2)
    private BigDecimal requestedQuantity;

    @Column(name = "requested_unit", length = 50)
    private String requestedUnit;

    @Column(name = "requested_brand", length = 100)
    private String requestedBrand;

    @Column(name = "dimensions", length = 100)
    private String dimensions;

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 30)
    @Builder.Default
    private MatchStatus matchStatus = MatchStatus.NOT_FOUND;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_product_id")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private Product matchedProduct;

    @Column(name = "candidate_product_ids", columnDefinition = "TEXT")
    private String candidateProductIds;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "applied_tier_description", length = 200)
    private String appliedTierDescription;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "line_subtotal", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lineSubtotal = BigDecimal.ZERO;

    @Column(name = "line_tax", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lineTax = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = false;

    @Column(name = "available_stock")
    private Integer availableStock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.matchStatus == null) {
            this.matchStatus = MatchStatus.NOT_FOUND;
        }
        if (this.lineSubtotal == null) {
            this.lineSubtotal = BigDecimal.ZERO;
        }
        if (this.lineTax == null) {
            this.lineTax = BigDecimal.ZERO;
        }
        if (this.lineTotal == null) {
            this.lineTotal = BigDecimal.ZERO;
        }
        if (this.isAvailable == null) {
            this.isAvailable = false;
        }
    }
}
