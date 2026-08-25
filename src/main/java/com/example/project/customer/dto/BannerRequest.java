package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerRequest {

    @NotBlank(message = "Title must not be blank")
    private String title;

    private String imageUrl;
    private String linkType;
    private String linkValue;
    private String position;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
