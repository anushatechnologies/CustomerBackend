package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotDealResponse {

    private Long id;

    private Integer productId;

    private Integer displayOrder;

    @JsonProperty("active")
    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private ProductResponse product;

    @JsonProperty("active")
    public Boolean isActive() {
        return active != null && active;
    }
}
