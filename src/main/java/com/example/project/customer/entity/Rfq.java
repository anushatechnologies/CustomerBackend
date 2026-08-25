package com.example.project.customer.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rfqs")
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rfq_id")
    private Integer rfqId;

    @Column(name = "rfq_number", nullable = false, unique = true)
    private String rfqNumber;

    @Column(name = "user_id", nullable = false)
    private Integer userId = 101;

    @Column(nullable = false)
    private String title;

    private String category;

    @Column(name = "product_material", nullable = false)
    private String productMaterial;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String unit;

    @Column(name = "technical_grade")
    private String technicalGrade;

    @Column(name = "mtc_required")
    private boolean mtcRequired = false;

    @Column(name = "delivery_location", nullable = false)
    private String deliveryLocation;

    @Column(name = "required_by_date")
    private LocalDate requiredByDate;

    @Column(name = "site_access")
    private String siteAccess;

    @Column(name = "crane_required")
    private boolean craneRequired = false;

    @Column(name = "target_budget", precision = 14, scale = 2)
    private BigDecimal targetBudget;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "boq_attachment_url")
    private String boqAttachmentUrl;

    @Column(nullable = false)
    private String status = "OPEN";

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<RfqQuotation> quotations = new ArrayList<>();

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<RfqQuestion> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Rfq() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public List<RfqQuotation> getQuotations() {
        return quotations;
    }

    public void setQuotations(List<RfqQuotation> quotations) {
        this.quotations = quotations;
    }

    public List<RfqQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<RfqQuestion> questions) {
        this.questions = questions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getQuotesCount() {
        return quotations != null ? quotations.size() : 0;
    }

    public void addQuotation(RfqQuotation quotation) {
        quotations.add(quotation);
        quotation.setRfq(this);
    }

    public void addQuestion(RfqQuestion question) {
        questions.add(question);
        question.setRfq(this);
    }
}
