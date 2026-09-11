package com.hinchexpress.trip.entity;

import com.hinchexpress.common.enums.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Authoritative Delivery Trip Entity in HinchExpress.
 * Core Invariant: One Order = One Trip (orderId is UNIQUE).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_number", nullable = false, unique = true, length = 64)
    private String tripNumber;

    @Column(name = "order_id", nullable = false, unique = true)
    private Integer orderId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "store_id", nullable = false)
    private Integer storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TripStatus status;

    // Assigned Driver & Vehicle details
    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "vehicle_number", length = 32)
    private String vehicleNumber;

    @Column(name = "driver_name", length = 128)
    private String driverName;

    @Column(name = "driver_phone", length = 32)
    private String driverPhone;

    // Pickup Store Coordinates
    @Column(name = "pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @Column(name = "pickup_address", length = 512)
    private String pickupAddress;

    // Customer Destination Coordinates
    @Column(name = "delivery_address_id")
    private Integer deliveryAddressId;

    @Column(name = "delivery_latitude", nullable = false)
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude", nullable = false)
    private Double deliveryLongitude;

    @Column(name = "delivery_address", length = 512)
    private String deliveryAddress;

    // Payload & Distance Metrics
    @Column(name = "total_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal totalWeightKg;

    @Column(name = "estimated_distance_km", precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    // Dual OTP Security Fields
    @Column(name = "pickup_otp", nullable = false, length = 6)
    private String pickupOtp;

    @Column(name = "delivery_otp", nullable = false, length = 6)
    private String deliveryOtp;

    @Column(name = "pickup_otp_verified", nullable = false)
    private boolean pickupOtpVerified;

    @Column(name = "delivery_otp_verified", nullable = false)
    private boolean deliveryOtpVerified;

    @Column(name = "otp_failed_attempts", nullable = false)
    private int otpFailedAttempts;

    // Milestone Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "arrived_at_pickup_at")
    private LocalDateTime arrivedAtPickupAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "arrived_at_delivery_at")
    private LocalDateTime arrivedAtDeliveryAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TripStatus.DISPATCH_PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}