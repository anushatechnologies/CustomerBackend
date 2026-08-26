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
public class OrderCreateRequest {

    @NotNull(message = "Address ID is required")
    private Integer addressId;

    @Builder.Default
    private String paymentMethod = "RAZORPAY";

    private String deliverySlot;
    private String deliveryInstructions;
    private String poNumber;

    @Builder.Default
    @JsonProperty("requiresCraneUnloading")
    private Boolean requiresCraneUnloading = false;
}
