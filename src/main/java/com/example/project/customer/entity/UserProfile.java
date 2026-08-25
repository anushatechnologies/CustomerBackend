package com.example.project.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Integer id = 101;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String role = "BUYER";

    @Column(nullable = false)
    private String tier = "GOLD";

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "is_gst_verified")
    private boolean isGstVerified = true;

    @Column(name = "credit_limit", precision = 14, scale = 2)
    private BigDecimal creditLimit = new BigDecimal("5000000.00");

    @Column(name = "available_credit", precision = 14, scale = 2)
    private BigDecimal availableCredit = new BigDecimal("3250000.00");

    public UserProfile() {
    }

    public UserProfile(Integer id, String fullName, String phone, String email, String role, String tier,
                       String companyName, String gstNumber, String panNumber, String businessType,
                       boolean isGstVerified, BigDecimal creditLimit, BigDecimal availableCredit) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.tier = tier;
        this.companyName = companyName;
        this.gstNumber = gstNumber;
        this.panNumber = panNumber;
        this.businessType = businessType;
        this.isGstVerified = isGstVerified;
        this.creditLimit = creditLimit;
        this.availableCredit = availableCredit;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public boolean isGstVerified() {
        return isGstVerified;
    }

    public void setGstVerified(boolean gstVerified) {
        isGstVerified = gstVerified;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getAvailableCredit() {
        return availableCredit;
    }

    public void setAvailableCredit(BigDecimal availableCredit) {
        this.availableCredit = availableCredit;
    }
}
