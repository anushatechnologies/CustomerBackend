package com.example.project.customer.dto.estimation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedRequirementList {

    @JsonAlias({"items", "requirements", "materials", "products"})
    @Builder.Default
    private List<ExtractedRequirementItem> items = new ArrayList<>();

    @JsonAlias({"projectSummary", "summary", "description"})
    private String projectSummary;
}
