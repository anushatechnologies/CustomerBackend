package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationOptionRequest {

    @NotBlank(message = "Option value is required")
    @JsonAlias({"optionValue"})
    private String value;

    @Builder.Default
    private Integer displayOrder = 0;
}
