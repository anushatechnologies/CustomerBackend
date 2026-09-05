package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "product_title", nullable = false)
    private String productTitle;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("18.00");

    @Column(name = "line_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal lineTotal;
}
