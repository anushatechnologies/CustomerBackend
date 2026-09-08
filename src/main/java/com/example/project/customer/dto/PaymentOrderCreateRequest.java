package com.example.project.customer.dto;

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
    private Integer orderId;

    /**
     * Target address ID for Checkout / Cart payment preview
     */
    private Integer addressId;

    /**
     * Optional delivery slot for checkout
     */
    private String deliverySlot;

    /**
     * Optional crane unloading requirement
     */
    private Boolean requiresCraneUnloading;

    /**
     * Optional amount override or required for wallet topup.
     * When paying for Order or Cart/Checkout, this is automatically fetched from backend.
     */
    @DecimalMin(value = "1.0", message = "Minimum amount is 1.00")
    private BigDecimal amount;

    /**
     * Purpose: ORDER_PAYMENT, CART_PAYMENT, CHECKOUT, or WALLET_TOPUP
     */
    @Builder.Default
    private String purpose = "ORDER_PAYMENT";
}
