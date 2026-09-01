package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_adjustments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjustment_id")
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "adjustment_type", nullable = false, length = 20)
    private String adjustmentType; // add, deduct, audit_correction

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "previous_stock")
    private Integer previousStock;

    @Column(name = "new_stock")
    private Integer newStock;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "adjusted_at", nullable = false, updatable = false)
    private LocalDateTime adjustedAt;
}
