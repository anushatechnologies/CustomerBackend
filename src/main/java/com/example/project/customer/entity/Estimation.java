package com.example.project.customer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estimations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estimation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estimation_id")
    private Long id;

    @Column(name = "estimation_number", nullable = false, unique = true, length = 50)
    private String estimationNumber;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "original_file_type", length = 50)
    private String originalFileType;

    @Column(name = "original_file_size")
    private Long originalFileSize;

    @Column(name = "original_file_url", columnDefinition = "TEXT")
    private String originalFileUrl;

    @Column(name = "original_file_key")
    private String originalFileKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EstimationStatus status = EstimationStatus.UPLOADED;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "quotation_pdf_url", columnDefinition = "TEXT")
    private String quotationPdfUrl;

    @Column(name = "quotation_pdf_key")
    private String quotationPdfKey;

    @Column(name = "subtotal", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "estimation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EstimationItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addItem(EstimationItem item) {
        items.add(item);
        item.setEstimation(this);
    }

    public void removeItem(EstimationItem item) {
        items.remove(item);
        item.setEstimation(null);
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = EstimationStatus.UPLOADED;
        }
        if (this.subtotal == null) {
            this.subtotal = BigDecimal.ZERO;
        }
        if (this.taxAmount == null) {
            this.taxAmount = BigDecimal.ZERO;
        }
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }
        if (this.grandTotal == null) {
            this.grandTotal = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
