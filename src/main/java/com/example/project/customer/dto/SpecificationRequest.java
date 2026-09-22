package com.example.project.customer.dto;

import com.example.project.customer.entity.SpecificationInputType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationRequest {

    @NotBlank(message = "Specification name is required")
    private String name;

    private String key;

    @NotNull(message = "Specification inputType is required")
    private SpecificationInputType inputType;

    private String unit;

    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;

    private List<String> options;
}
