package com.example.project.customer.dto;

import java.math.BigDecimal;

public class CheckoutPreviewResponse {

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

    public CheckoutPreviewResponse() {
    }

    public CheckoutPreviewResponse(BigDecimal subtotal, BigDecimal discount, BigDecimal taxableAmount,
                                   BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal totalGst,
                                   BigDecimal freightCharge, BigDecimal craneUnloadingCharge, BigDecimal grandTotal) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.taxableAmount = taxableAmount;
        this.cgst = cgst;
        this.sgst = sgst;
        this.igst = igst;
        this.totalGst = totalGst;
        this.freightCharge = freightCharge;
        this.craneUnloadingCharge = craneUnloadingCharge;
        this.grandTotal = grandTotal;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getCgst() {
        return cgst;
    }

    public void setCgst(BigDecimal cgst) {
        this.cgst = cgst;
    }

    public BigDecimal getSgst() {
        return sgst;
    }

    public void setSgst(BigDecimal sgst) {
        this.sgst = sgst;
    }

    public BigDecimal getIgst() {
        return igst;
    }

    public void setIgst(BigDecimal igst) {
        this.igst = igst;
    }

    public BigDecimal getTotalGst() {
        return totalGst;
    }

    public void setTotalGst(BigDecimal totalGst) {
        this.totalGst = totalGst;
    }

    public BigDecimal getFreightCharge() {
        return freightCharge;
    }

    public void setFreightCharge(BigDecimal freightCharge) {
        this.freightCharge = freightCharge;
    }

    public BigDecimal getCraneUnloadingCharge() {
        return craneUnloadingCharge;
    }

    public void setCraneUnloadingCharge(BigDecimal craneUnloadingCharge) {
        this.craneUnloadingCharge = craneUnloadingCharge;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }
}
