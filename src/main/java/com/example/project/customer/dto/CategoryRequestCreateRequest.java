package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestCreateRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private Integer parentCategoryId;
    private String description;
}
