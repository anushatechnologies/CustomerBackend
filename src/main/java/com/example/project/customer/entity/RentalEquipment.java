package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "rental_equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id")
    private Integer equipmentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category; // EXCAVATOR, CRANE, SCAFFOLDING, GENERATOR, CONCRETE_MIXER, BOBCAT

    @Column(name = "model")
    private String model;

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "daily_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal dailyRate;

    @Column(name = "weekly_rate", precision = 12, scale = 2)
    private BigDecimal weeklyRate;

    @Column(name = "monthly_rate", precision = 12, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "location")
    private String location; // Hyderabad, Bengaluru, Chennai, etc.

    @Column(name = "operator_available")
    @Builder.Default
    private Boolean operatorAvailable = true;

    @Column(name = "operator_daily_charge", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal operatorDailyCharge = new BigDecimal("1200.00");

    @Column(name = "is_available")
    @Builder.Default
    private Boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
