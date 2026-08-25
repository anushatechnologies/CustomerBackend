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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id")
    @Builder.Default
    private Integer userId = 101;

    @Column(name = "address_id")
    private Integer addressId;

    @Column(name = "delivery_location")
    private String deliveryLocation;

    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "taxable_amount", precision = 14, scale = 2)
    private BigDecimal taxableAmount;

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

    @Column(name = "freight_charge", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal freightCharge = BigDecimal.ZERO;

    @Column(name = "crane_unloading_charge", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal craneUnloadingCharge = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method")
    @Builder.Default
    private String paymentMethod = "RAZORPAY";

    @Column(name = "payment_status")
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "order_status")
    @Builder.Default
    private String orderStatus = "PLACED";

    @Column(name = "po_number")
    private String poNumber;

    @Column(name = "delivery_slot")
    private String deliverySlot;

    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    private String deliveryInstructions;

    @Column(name = "requires_crane_unloading")
    @Builder.Default
    @JsonProperty("requiresCraneUnloading")
    private boolean requiresCraneUnloading = false;

    @Column(name = "carrier_name")
    private String carrierName;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    @Builder.Default
    private List<TrackingCheckpoint> checkpoints = new ArrayList<>();

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
        if (this.orderNumber == null) {
            this.orderNumber = "ORD-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
