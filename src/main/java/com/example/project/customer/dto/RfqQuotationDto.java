package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RfqQuotationDto {

    private Integer quoteId;
    private Integer rfqId;
    private Integer vendorId;
    private String vendorName;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer deliveryLeadTimeDays;
    private String paymentTermsOffered;
    private boolean mtcIncluded;
    private boolean freightIncluded;
    private LocalDateTime validUntil;
    private Double vendorRating;
    private String status;

    public RfqQuotationDto() {
    }

    public RfqQuotationDto(Integer quoteId, Integer rfqId, Integer vendorId, String vendorName,
                           BigDecimal unitPrice, BigDecimal totalAmount, Integer deliveryLeadTimeDays,
                           String paymentTermsOffered, boolean mtcIncluded, boolean freightIncluded,
                           LocalDateTime validUntil, Double vendorRating, String status) {
        this.quoteId = quoteId;
        this.rfqId = rfqId;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.deliveryLeadTimeDays = deliveryLeadTimeDays;
        this.paymentTermsOffered = paymentTermsOffered;
        this.mtcIncluded = mtcIncluded;
        this.freightIncluded = freightIncluded;
        this.validUntil = validUntil;
        this.vendorRating = vendorRating;
        this.status = status;
    }

    public Integer getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Integer quoteId) {
        this.quoteId = quoteId;
    }

    public Integer getRfqId() {
        return rfqId;
    }

    public void setRfqId(Integer rfqId) {
        this.rfqId = rfqId;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getDeliveryLeadTimeDays() {
        return deliveryLeadTimeDays;
    }

    public void setDeliveryLeadTimeDays(Integer deliveryLeadTimeDays) {
        this.deliveryLeadTimeDays = deliveryLeadTimeDays;
    }

    public String getPaymentTermsOffered() {
        return paymentTermsOffered;
    }

    public void setPaymentTermsOffered(String paymentTermsOffered) {
        this.paymentTermsOffered = paymentTermsOffered;
    }

    public boolean isMtcIncluded() {
        return mtcIncluded;
    }

    public void setMtcIncluded(boolean mtcIncluded) {
        this.mtcIncluded = mtcIncluded;
    }

    public boolean isFreightIncluded() {
        return freightIncluded;
    }

    public void setFreightIncluded(boolean freightIncluded) {
        this.freightIncluded = freightIncluded;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public Double getVendorRating() {
        return vendorRating;
    }

    public void setVendorRating(Double vendorRating) {
        this.vendorRating = vendorRating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
