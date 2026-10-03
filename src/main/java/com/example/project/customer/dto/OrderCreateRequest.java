package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @JsonAlias({"deliveryAddressId", "delivery_address_id", "shippingAddressId", "shipping_address_id"})
    private Integer addressId;

    @Builder.Default
    @JsonAlias({"payment_method"})
    private String paymentMethod = "RAZORPAY";

    @JsonAlias({"delivery_slot"})
    private String deliverySlot;

    @JsonAlias({"delivery_instructions"})
    private String deliveryInstructions;

    @JsonAlias({"po_number"})
    private String poNumber;

    @Builder.Default
    @JsonProperty("requiresCraneUnloading")
    @JsonAlias({"requires_crane_unloading", "craneUnloading", "crane_unloading"})
    private Boolean requiresCraneUnloading = false;
}
