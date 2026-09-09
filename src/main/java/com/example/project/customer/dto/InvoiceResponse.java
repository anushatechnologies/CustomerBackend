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
public class InvoiceResponse {
    private String invoiceNumber;
    private Integer orderId;
    private String orderNumber;
    private String invoiceDate;
    private String supplierName;
    private String supplierGstin;
    private String supplierAddress;
    private String sellerLegalName;
    private String sellerGstin;
    private String recipientName;
    private String recipientAddress;
    private String recipientGstin;
    private String buyerLegalName;
    private String buyerGstin;
    private String placeOfSupply;
    private String paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal totalGst;
    private BigDecimal freightCharge;
    private BigDecimal craneUnloadingCharge;
    private BigDecimal grandTotal;
    private String pdfUrl;

    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItem {
        private Integer itemId;
        private String description;
        private String hsnCode;
        private Integer quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private BigDecimal gstRate;
        private BigDecimal gstAmount;
    }
}
