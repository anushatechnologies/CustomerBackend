package com.example.project.customer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id")
    private Integer poId;

    @Column(name = "po_number", nullable = false, unique = true)
    private String poNumber;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "vendor_id", nullable = false)
    private Integer vendorId;

    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING_APPROVAL"; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, COMPLETED

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "payment_terms")
    @Builder.Default
    private String paymentTerms = "NET_30";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

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
        if (this.poNumber == null) {
            this.poNumber = "PO-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
