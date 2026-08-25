package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rfqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rfq_id")
    private Integer rfqId;

    @Column(name = "rfq_number", nullable = false, unique = true)
    private String rfqNumber;

    @Column(name = "user_id")
    @Builder.Default
    private Integer userId = 101;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(name = "product_material", nullable = false)
    private String productMaterial;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String unit;

    @Column(name = "technical_grade")
    private String technicalGrade;

    @Column(name = "mtc_required")
    @Builder.Default
    @JsonProperty("mtcRequired")
    private boolean mtcRequired = true;

    @Column(name = "delivery_location", nullable = false)
    private String deliveryLocation;

    @Column(name = "required_by_date")
    private LocalDate requiredByDate;

    @Column(name = "site_access")
    private String siteAccess;

    @Column(name = "crane_required")
    @Builder.Default
    @JsonProperty("craneRequired")
    private boolean craneRequired = false;

    @Column(name = "target_budget", precision = 14, scale = 2)
    private BigDecimal targetBudget;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "boq_attachment_url")
    private String boqAttachmentUrl;

    @Column(name = "status")
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "quotes_count")
    @Builder.Default
    private Integer quotesCount = 0;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Quotation> quotations = new ArrayList<>();

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<RfqQuestion> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.rfqNumber == null) {
            this.rfqNumber = "RFQ-" + System.currentTimeMillis();
        }
        if (this.status == null) {
            this.status = "OPEN";
        }
        if (this.quotesCount == null) {
            this.quotesCount = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
