package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderCreateRequest {

    /**
     * Target order ID to pay for (if paying for an already placed order)
     */
    @JsonAlias({"order_id"})
    private Integer orderId;

    /**
     * Target address ID for Checkout / Cart payment preview
     */
    @JsonAlias({"address_id"})
    private Integer addressId;

    /**
     * Optional delivery slot for checkout
     */
    @JsonAlias({"delivery_slot"})
    private String deliverySlot;

    /**
     * Optional crane unloading requirement
     */
    @JsonAlias({"requires_crane_unloading", "crane_unloading"})
    private Boolean requiresCraneUnloading;

    /**
     * Optional amount override or required for wallet topup.
     * When paying for Order or Cart/Checkout, this is automatically fetched from backend.
     */
    @DecimalMin(value = "0.01", message = "Minimum amount is 0.01")
    private BigDecimal amount;

    /**
     * Purpose: ORDER_PAYMENT, CART_PAYMENT, CHECKOUT, or WALLET_TOPUP
     */
    @Builder.Default
    private String purpose = "ORDER_PAYMENT";
}
