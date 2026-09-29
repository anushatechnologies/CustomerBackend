package com.example.project.customer.entity;

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
@Table(name = "rider_dispatch_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderDispatchOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private DeliveryRider rider;

    @Column(name = "sequence_index", nullable = false)
    @Builder.Default
    private int sequenceIndex = 1;

    @Column(name = "distance_km", nullable = false)
    @Builder.Default
    private double distanceKm = 0.0;

    @Column(name = "offered_fare", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal offeredFare = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "OFFERED";

    @Column(name = "offered_at", nullable = false)
    private LocalDateTime offeredAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @PrePersist
    void onCreate() {
        if (this.offeredAt == null) {
            this.offeredAt = LocalDateTime.now();
        }
    }
}
