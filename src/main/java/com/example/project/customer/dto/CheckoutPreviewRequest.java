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
public class CheckoutPreviewRequest {

    @NotNull(message = "Address ID is required")
    private Integer addressId;

    private String deliverySlot;

    @Builder.Default
    @JsonProperty("requiresCraneUnloading")
    private Boolean requiresCraneUnloading = false;
}
