package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartStoreGroupResponse {
    private Integer storeId;
    private String storeName;
    private String storeSlug;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal totalGst;
    private BigDecimal deliveryCharge;
    private BigDecimal craneUnloadingCharge;
    private BigDecimal grandTotal;
    private Integer itemCount;
    private Boolean canCheckout;
    private String checkoutBlockReason;
}
