package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_quotations")
public class RfqQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Integer quoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    @JsonBackReference
    private Rfq rfq;

    @Column(name = "vendor_id", nullable = false)
    private Integer vendorId;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_lead_time_days")
    private Integer deliveryLeadTimeDays = 5;

    @Column(name = "payment_terms_offered")
    private String paymentTermsOffered;

    @Column(name = "mtc_included")
    private boolean mtcIncluded = true;

    @Column(name = "freight_included")
    private boolean freightIncluded = true;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "vendor_rating")
    private Double vendorRating = 4.8;

    @Column(nullable = false)
    private String status = "PENDING";

    public RfqQuotation() {
    }

    public RfqQuotation(Integer vendorId, String vendorName, BigDecimal unitPrice, BigDecimal totalAmount,
                        Integer deliveryLeadTimeDays, String paymentTermsOffered, boolean mtcIncluded,
                        boolean freightIncluded, LocalDateTime validUntil, Double vendorRating, String status) {
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

    public Rfq getRfq() {
        return rfq;
    }

    public void setRfq(Rfq rfq) {
        this.rfq = rfq;
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
