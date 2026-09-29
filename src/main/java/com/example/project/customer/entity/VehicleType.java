package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "max_weight_kg", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxWeightKg = BigDecimal.valueOf(50.00);

    @Column(name = "size_dimensions", length = 100)
    private String sizeDimensions;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseFare = BigDecimal.valueOf(40.00);

    @Column(name = "base_distance_km", nullable = false)
    @Builder.Default
    private double baseDistanceKm = 2.0;

    @Column(name = "per_km_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal perKmRate = BigDecimal.valueOf(12.00);

    @Column(name = "minimum_fare", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumFare = BigDecimal.valueOf(40.00);

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "image_url", length = 2000)
    private String imageUrl;

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
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
