package com.example.project.customer.dto;

import java.time.LocalDateTime;

public class BannerResponse {

    private final Integer bannerId;
    private final String title;
    private final String imageUrl;
    private final String linkType;
    private final String linkValue;
    private final String position;
    private final Integer sortOrder;
    private final Boolean isActive;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final LocalDateTime createdAt;

    public BannerResponse(Integer bannerId, String title, String imageUrl, String linkType,
                          String linkValue, String position, Integer sortOrder, Boolean isActive,
                          LocalDateTime startDate, LocalDateTime endDate, LocalDateTime createdAt) {
        this.bannerId = bannerId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkType = linkType;
        this.linkValue = linkValue;
        this.position = position;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public Integer getBannerId() {
        return bannerId;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public String getPosition() {
        return position;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
