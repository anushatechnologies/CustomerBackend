package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryRequest {

    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    @NotBlank(message = "Subcategory name is required")
    private String name;

    private String slug;

    @com.example.project.customer.validation.ValidImageUrl
    private String imageUrl;

    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;

    @Builder.Default
    @JsonProperty("visibleOnWebsite")
    private Boolean visibleOnWebsite = true;

    @Builder.Default
    private Integer sortOrder = 0;
}