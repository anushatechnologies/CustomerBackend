package com.example.project.customer.dto;

import com.example.project.customer.entity.SpecificationInputType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpecificationResponse {

    private Integer id; // specificationId
    private Integer mappingId; // category_spec_id
    private Integer categoryId;
    private String name;
    private String key;
    private SpecificationInputType inputType;
    private String unit;

    @JsonProperty("required")
    private Boolean required;

    private Integer displayOrder;

    @JsonProperty("active")
    private Boolean active;

    @Builder.Default
    private List<String> options = new ArrayList<>();
}
