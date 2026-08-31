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
public class BrandResponse {

    private Integer brandId;

    private Integer subcategoryId;

    private String subcategoryName;

    private Integer categoryId;

    private String categoryName;

    private String name;

    private String slug;

    private String imageUrl;

    @JsonProperty("active")
    private boolean active;

    private Integer sortOrder;

    private Integer productCount;

    private LocalDateTime createdAt;
}
