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
public class BlogArticleResponse {
    private Integer articleId;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String author;
    private String category;
    private String tags;
    private Integer readTimeMinutes;
    private String imageUrl;
    private Boolean published;
    private LocalDateTime publishedAt;
}
