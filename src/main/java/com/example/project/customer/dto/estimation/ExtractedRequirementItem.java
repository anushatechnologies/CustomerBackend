package com.example.project.customer.dto.estimation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedRequirementItem {

    @JsonAlias({"name", "productName", "itemName", "materialName"})
    private String name;

    @JsonAlias({"quantity", "qty", "amount"})
    private BigDecimal quantity;

    @JsonAlias({"unit", "uom", "measurement"})
    private String unit;

    @JsonAlias({"specification", "specifications", "grade", "specs"})
    private String specification;

    @JsonAlias({"brand", "company", "manufacturer"})
    private String brand;

    @JsonAlias({"dimensions", "dimension", "size"})
    private String dimensions;

    @JsonAlias({"notes", "note", "remark", "remarks"})
    private String notes;
}
