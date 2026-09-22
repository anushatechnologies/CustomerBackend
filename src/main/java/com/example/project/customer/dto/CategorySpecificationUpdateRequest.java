package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpecificationUpdateRequest {

    @JsonProperty("required")
    private Boolean required;

    private Integer displayOrder;

    @JsonProperty("active")
    private Boolean active;
}
