package com.example.project.customer.dto;

import com.example.project.customer.entity.SpecificationInputType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationResponse {

    private Integer id;
    private String name;
    private String key;
    private SpecificationInputType inputType;
    private String unit;

    @JsonProperty("active")
    private Boolean active;

    @Builder.Default
    private List<SpecificationOptionResponse> options = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
