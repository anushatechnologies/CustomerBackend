package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "subcategories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subcategory_id")
    private Integer subcategoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    @JsonProperty("active")
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "product_count", nullable = false)
    @Builder.Default
    private Integer productCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void setCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.productCount == null) {
            this.productCount = 0;
        }
    }
}