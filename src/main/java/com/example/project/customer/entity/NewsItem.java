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

import java.time.LocalDateTime;

@Entity
@Table(name = "news_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Integer newsId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "category")
    @Builder.Default
    private String category = "COMMODITY_PRICES"; // COMMODITY_PRICES, INFRASTRUCTURE, MARKET_TRENDS

    @Column(name = "source")
    private String source; // "Steel Authority of India", "Ministry of Commerce", "HinchMart Market Intelligence"

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "price_change_percentage")
    private Double priceChangePercentage;

    @Column(name = "trend_direction")
    private String trendDirection; // UP, DOWN, STABLE

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }
}
