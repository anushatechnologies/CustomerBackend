package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BannerResponse {

    private Integer bannerId;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkType;
    private String linkValue;
    private String position;
    private Integer sortOrder;

    @JsonProperty("active")
    private Boolean active;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    public BannerResponse() {
    }

    public BannerResponse(Integer bannerId, String title, String imageUrl, String linkType,
                          String linkValue, String position, Integer sortOrder, Boolean active,
                          LocalDateTime startDate, LocalDateTime endDate, LocalDateTime createdAt) {
        this(bannerId, title, null, imageUrl, linkType, linkValue, position, sortOrder, active, startDate, endDate, createdAt);
    }

    public BannerResponse(Integer bannerId, String title, String subtitle, String imageUrl, String linkType,
                          String linkValue, String position, Integer sortOrder, Boolean active,
                          LocalDateTime startDate, LocalDateTime endDate, LocalDateTime createdAt) {
        this.bannerId = bannerId;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.linkType = linkType;
        this.linkValue = linkValue;
        this.position = position;
        this.sortOrder = sortOrder;
        this.active = active;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public Integer getBannerId() {
        return bannerId;
    }

    public void setBannerId(Integer bannerId) {
        this.bannerId = bannerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public void setLinkValue(String linkValue) {
        this.linkValue = linkValue;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @JsonProperty("active")
    public Boolean getActive() {
        return active;
    }

    @JsonProperty("active")
    public void setActive(Boolean active) {
        this.active = active;
    }

    @JsonProperty("isActive")
    public Boolean getIsActive() {
        return active;
    }

    @JsonProperty("isActive")
    public void setIsActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
