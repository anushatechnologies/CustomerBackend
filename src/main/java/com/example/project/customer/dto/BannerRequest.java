package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class BannerRequest {

    @NotBlank(message = "Title must not be blank")
    private String title;

    private String subtitle;
    private String imageUrl;
    private String linkType;
    private String linkValue;
    private String position;
    private Integer sortOrder;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("isActive")
    private Boolean isActive;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public BannerRequest() {
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

    public Boolean getActive() {
        if (active != null) return active;
        return isActive != null ? isActive : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
        this.isActive = active;
    }

    public Boolean getIsActive() {
        return getActive();
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.active = isActive;
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
}
