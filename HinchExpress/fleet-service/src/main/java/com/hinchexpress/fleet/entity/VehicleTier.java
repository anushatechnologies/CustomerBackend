package com.hinchexpress.fleet.entity;

import com.hinchexpress.common.enums.VehicleTierCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicle_tiers")
public class VehicleTier {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 50, nullable = false)
    private VehicleTierCode code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "max_payload_kg", precision = 12, scale = 3, nullable = false)
    private BigDecimal maxPayloadKg;

    @Column(name = "volumetric_capacity_m3", precision = 10, scale = 2)
    private BigDecimal volumetricCapacityM3;

    @Builder.Default
    @Column(name = "base_fare", precision = 10, scale = 2, nullable = false)
    private BigDecimal baseFare = new BigDecimal("150.00");

    @Builder.Default
    @Column(name = "per_km_rate", precision = 10, scale = 2, nullable = false)
    private BigDecimal perKmRate = new BigDecimal("25.00");

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
