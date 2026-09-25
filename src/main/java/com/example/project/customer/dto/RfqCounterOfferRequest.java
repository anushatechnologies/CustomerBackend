package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfqCounterOfferRequest {

    @JsonProperty("counterPrice")
    private BigDecimal counterPrice;

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("notes")
    private String notes;
}
