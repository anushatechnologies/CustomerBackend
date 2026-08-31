package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequest {

    @NotNull(message = "Subcategory ID is required")
    @Positive(message = "Subcategory ID must be positive")
    private Integer subcategoryId;

    @NotBlank(message = "Brand name is required")
    private String name;

    private String slug;

    private String imageUrl;

    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;

    @Builder.Default
    private Integer sortOrder = 0;
}
