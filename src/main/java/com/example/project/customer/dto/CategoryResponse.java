package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private Integer categoryId;
    private String name;
    private String slug;
    private String imageUrl;
    private boolean active = true;
    private Integer sortOrder = 0;
    private Long productCount;
    private List<SubcategoryResponse> subcategories;
    private LocalDateTime createdAt;

    public CategoryResponse() {
    }

    public CategoryResponse(Integer categoryId, String name, String slug, String imageUrl,
                            boolean active, Integer sortOrder, Long productCount,
                            List<SubcategoryResponse> subcategories, LocalDateTime createdAt) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.imageUrl = imageUrl;
        this.active = active;
        this.sortOrder = sortOrder;
        this.productCount = productCount;
        this.subcategories = subcategories;
        this.createdAt = createdAt;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public List<SubcategoryResponse> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<SubcategoryResponse> subcategories) {
        this.subcategories = subcategories;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}