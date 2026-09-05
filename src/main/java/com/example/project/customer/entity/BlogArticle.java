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
@Table(name = "blog_articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id")
    private Integer articleId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "excerpt", columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "author")
    private String author;

    @Column(name = "category")
    private String category; // PROCUREMENT, STRUCTURAL_DESIGN, MATERIAL_TESTING, ESG

    @Column(name = "tags")
    private String tags; // Comma-separated: "steel,tmt,procurement"

    @Column(name = "read_time_minutes")
    @Builder.Default
    private Integer readTimeMinutes = 5;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_published")
    @Builder.Default
    private Boolean published = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.publishedAt == null && Boolean.TRUE.equals(this.published)) {
            this.publishedAt = LocalDateTime.now();
        }
    }
}
