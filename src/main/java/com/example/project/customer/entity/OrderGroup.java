package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "order_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "group_order_number", nullable = false, unique = true, length = 100)
    private String groupOrderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "address_id")
    private Integer addressId;

    @Column(name = "delivery_location", columnDefinition = "TEXT")
    private String deliveryLocation;

    @Column(name = "order_for_someone_else")
    @Builder.Default
    private Boolean orderForSomeoneElse = false;

    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;

    @Column(name = "subtotal", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_discount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 100)
    private String couponCode;

    @Column(name = "taxable_amount", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "cgst", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(name = "sgst", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(name = "igst", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal igst = BigDecimal.ZERO;

    @Column(name = "total_gst", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalGst = BigDecimal.ZERO;

    @Column(name = "total_freight", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalFreight = BigDecimal.ZERO;

    @Column(name = "total_crane_charge", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCraneCharge = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 50)
    @Builder.Default
    private String paymentMethod = "RAZORPAY";

    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "group_status", length = 50)
    @Builder.Default
    private String groupStatus = "PLACED";

    @OneToMany(mappedBy = "orderGroup", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

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
        if (this.groupOrderNumber == null) {
            this.groupOrderNumber = "GRP-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
