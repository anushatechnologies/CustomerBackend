package com.example.project.customer.entity;

import com.example.project.customer.entity.converter.BulkPricingTiersConverter;
import com.example.project.customer.entity.converter.StringListConverter;
import com.example.project.customer.entity.converter.StringMapConverter;
import com.example.project.customer.entity.converter.VendorInfoConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private Brand brand;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String slug;

    @Column(name = "sku")
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "images", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(nullable = false)
    private String unit;

    @Column(name = "moq")
    @Builder.Default
    private Integer moq = 1;

    @Column(name = "stock_qty")
    @Builder.Default
    private Integer stockQty = 0;

    @Column(name = "is_active")
    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;

    @Column(name = "is_24_hour_delivery")
    @Builder.Default
    @JsonProperty("is24HourDelivery")
    private Boolean is24HourDelivery = false;

    @Column(name = "rating")
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = BigDecimal.valueOf(18.0);

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "specifications", columnDefinition = "TEXT")
    @Convert(converter = StringMapConverter.class)
    @Builder.Default
    private Map<String, String> specifications = new LinkedHashMap<>();
@Enumerated(EnumType.STRING)
@Column(name = "approval_status", nullable = false)
@Builder.Default
private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

@Column(name = "rejection_reason")
private String rejectionReason;

@Column(name = "bulk_pricing_tiers", columnDefinition = "TEXT")
@Convert(converter = BulkPricingTiersConverter.class)
@Builder.Default
private List<BulkPricingTier> bulkPricingTiers = new ArrayList<>();

@Column(name = "vendor_info", columnDefinition = "TEXT")
@Convert(converter = VendorInfoConverter.class)
private VendorInfo vendor;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("is24HourDelivery")
    public Boolean is24HourDelivery() {
        return is24HourDelivery != null && is24HourDelivery;
    }

    @JsonProperty("is24HourDelivery")
    public void setIs24HourDelivery(Boolean is24HourDelivery) {
        this.is24HourDelivery = is24HourDelivery != null ? is24HourDelivery : false;
    }

    @JsonProperty("active")
    public Boolean isActive() {
        return active != null && active;
    }

    @JsonProperty("active")
    public void setActive(Boolean active) {
        this.active = active != null ? active : true;
    }

    @PrePersist
    void setCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.slug == null && this.title != null) {
            this.slug = this.title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        }
        if (this.gstRate == null) {
            this.gstRate = BigDecimal.valueOf(18.0);
        }
        if (this.moq == null) {
            this.moq = 1;
        }
        if (this.stockQty == null) {
            this.stockQty = 0;
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.is24HourDelivery == null) {
            this.is24HourDelivery = false;
        }
        if (this.sellingPrice == null && this.price != null) {
            this.sellingPrice = this.price;
        }
        if (this.price == null && this.sellingPrice != null) {
            this.price = this.sellingPrice;
        }
    }

    public Subcategory getSubcategory() {
        return brand != null ? brand.getSubcategory() : null;
    }

    public Category getCategory() {
        return (brand != null && brand.getSubcategory() != null) ? brand.getSubcategory().getCategory() : null;
    }

    public String getBrandName() {
        return brand != null ? brand.getName() : null;
    }
}