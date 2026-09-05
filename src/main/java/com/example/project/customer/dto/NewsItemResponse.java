package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class NewsItemResponse {
    private Integer newsId;
    private String title;
    private String summary;
    private String content;
    private String category;
    private String source;
    private String sourceUrl;
    private String imageUrl;
    private Double priceChangePercentage;
    private String trendDirection;
    private LocalDateTime publishedAt;
}
