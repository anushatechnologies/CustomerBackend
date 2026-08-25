package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequest {

    @NotBlank(message = "Banner title is required")
    private String title;

    private String subtitle;

    private String imageUrl;

    private String linkType;

    private String linkValue;

    private String position;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;

    @JsonProperty("isActive")
    public void setIsActive(Boolean isActive) {
        this.active = isActive;
    }

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
