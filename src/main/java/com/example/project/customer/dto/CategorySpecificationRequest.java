package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpecificationRequest {

    @NotNull(message = "Specification ID is required")
    private Integer specificationId;

    @Builder.Default
    @JsonProperty("required")
    private Boolean required = false;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;
}
