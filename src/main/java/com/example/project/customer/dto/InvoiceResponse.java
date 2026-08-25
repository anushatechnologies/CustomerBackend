package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceResponse {

    private String invoiceNumber;
    private String orderNumber;
    private LocalDate invoiceDate;
    private String sellerGstin;
    private String sellerLegalName;
    private String buyerGstin;
    private String buyerLegalName;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal grandTotal;
    private String pdfUrl;

    public InvoiceResponse() {
    }

    public InvoiceResponse(String invoiceNumber, String orderNumber, LocalDate invoiceDate, String sellerGstin,
                           String sellerLegalName, String buyerGstin, String buyerLegalName,
                           BigDecimal taxableAmount, BigDecimal cgst, BigDecimal sgst, BigDecimal igst,
                           BigDecimal grandTotal, String pdfUrl) {
        this.invoiceNumber = invoiceNumber;
        this.orderNumber = orderNumber;
        this.invoiceDate = invoiceDate;
        this.sellerGstin = sellerGstin;
        this.sellerLegalName = sellerLegalName;
        this.buyerGstin = buyerGstin;
        this.buyerLegalName = buyerLegalName;
        this.taxableAmount = taxableAmount;
        this.cgst = cgst;
        this.sgst = sgst;
        this.igst = igst;
        this.grandTotal = grandTotal;
        this.pdfUrl = pdfUrl;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getSellerGstin() {
        return sellerGstin;
    }

    public void setSellerGstin(String sellerGstin) {
        this.sellerGstin = sellerGstin;
    }

    public String getSellerLegalName() {
        return sellerLegalName;
    }

    public void setSellerLegalName(String sellerLegalName) {
        this.sellerLegalName = sellerLegalName;
    }

    public String getBuyerGstin() {
        return buyerGstin;
    }

    public void setBuyerGstin(String buyerGstin) {
        this.buyerGstin = buyerGstin;
    }

    public String getBuyerLegalName() {
        return buyerLegalName;
    }

    public void setBuyerLegalName(String buyerLegalName) {
        this.buyerLegalName = buyerLegalName;
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

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}
