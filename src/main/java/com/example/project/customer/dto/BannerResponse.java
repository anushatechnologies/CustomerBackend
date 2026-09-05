package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
