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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private RentalEquipment equipment;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "site_address", columnDefinition = "TEXT", nullable = false)
    private String siteAddress;

    @Column(name = "operator_required")
    @Builder.Default
    private Boolean operatorRequired = false;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "rate_per_day", precision = 12, scale = 2, nullable = false)
    private BigDecimal ratePerDay;

    @Column(name = "operator_cost", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal operatorCost = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalCost;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "CONFIRMED"; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
