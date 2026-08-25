package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RfqResponse {

    private Integer rfqId;
    private String rfqNumber;
    private String title;
    private String category;
    private String productMaterial;
    private Integer quantity;
    private String unit;
    private String technicalGrade;
    private boolean mtcRequired;
    private String deliveryLocation;
    private LocalDate requiredByDate;
    private String siteAccess;
    private boolean craneRequired;
    private BigDecimal targetBudget;
    private String paymentTerms;
    private String specifications;
    private String boqAttachmentUrl;
    private String status;
    private Integer quotesCount;
    private LocalDateTime createdAt;

    public RfqResponse() {
    }

    public Integer getRfqId() {
        return rfqId;
    }

    public void setRfqId(Integer rfqId) {
        this.rfqId = rfqId;
    }

    public String getRfqNumber() {
        return rfqNumber;
    }

    public void setRfqNumber(String rfqNumber) {
        this.rfqNumber = rfqNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProductMaterial() {
        return productMaterial;
    }

    public void setProductMaterial(String productMaterial) {
        this.productMaterial = productMaterial;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTechnicalGrade() {
        return technicalGrade;
    }

    public void setTechnicalGrade(String technicalGrade) {
        this.technicalGrade = technicalGrade;
    }

    public boolean isMtcRequired() {
        return mtcRequired;
    }

    public void setMtcRequired(boolean mtcRequired) {
        this.mtcRequired = mtcRequired;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public LocalDate getRequiredByDate() {
        return requiredByDate;
    }

    public void setRequiredByDate(LocalDate requiredByDate) {
        this.requiredByDate = requiredByDate;
    }

    public String getSiteAccess() {
        return siteAccess;
    }

    public void setSiteAccess(String siteAccess) {
        this.siteAccess = siteAccess;
    }

    public boolean isCraneRequired() {
        return craneRequired;
    }

    public void setCraneRequired(boolean craneRequired) {
        this.craneRequired = craneRequired;
    }

    public BigDecimal getTargetBudget() {
        return targetBudget;
    }

    public void setTargetBudget(BigDecimal targetBudget) {
        this.targetBudget = targetBudget;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public String getBoqAttachmentUrl() {
        return boqAttachmentUrl;
    }

    public void setBoqAttachmentUrl(String boqAttachmentUrl) {
        this.boqAttachmentUrl = boqAttachmentUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getQuotesCount() {
        return quotesCount;
    }

    public void setQuotesCount(Integer quotesCount) {
        this.quotesCount = quotesCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
