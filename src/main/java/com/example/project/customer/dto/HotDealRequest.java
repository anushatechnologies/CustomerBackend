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
public class HotDealRequest {

    @NotNull(message = "Product ID is required")
    private Integer productId;

    private Integer displayOrder;

    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;

    @JsonProperty("active")
    public Boolean isActive() {
        return active != null && active;
    }

    @JsonProperty("active")
    public void setActive(Boolean active) {
        this.active = active != null ? active : true;
    }
}
