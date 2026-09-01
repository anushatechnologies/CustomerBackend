package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProduct {

    @Id
    @Column(name = "product_id", length = 50)
    private String id;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "subcategory_id")
    private Integer subcategoryId;

    @Column(name = "subcategory_name")
    private String subcategoryName;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "selling_price", nullable = false)
    private Long sellingPrice;

    @Column(name = "mrp", nullable = false)
    private Long mrp;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "moq")
    private Integer moq;

    @Column(name = "stock_qty")
    private Integer stockQty;

    @Column(name = "is_24hour_delivery")
    private Boolean is24HourDelivery;

    @Column(name = "status", length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "images", columnDefinition = "JSON")
    private String images; // JSON array

    @Column(name = "bulk_pricing_tiers", columnDefinition = "JSON")
    private String bulkPricingTiers; // JSON array

    @Column(name = "specifications", columnDefinition = "JSON")
    private String specifications; // JSON object

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
}
