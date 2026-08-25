package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerResponse {

    private Integer bannerId;
    private String title;
    private String imageUrl;
    private String linkType;
    private String linkValue;
    private String position;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
}
