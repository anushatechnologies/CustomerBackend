package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusinessDetailsDto {
    private String companyName;
    private String gstNumber;
    private String panNumber;

    @JsonProperty("isGstVerified")
    private boolean isGstVerified;

    private BigDecimal creditLimit;
    private BigDecimal availableCredit;

    public BusinessDetailsDto() {
    }

    public BusinessDetailsDto(String companyName, String gstNumber, String panNumber, boolean isGstVerified,
                              BigDecimal creditLimit, BigDecimal availableCredit) {
        this.companyName = companyName;
        this.gstNumber = gstNumber;
        this.panNumber = panNumber;
        this.isGstVerified = isGstVerified;
        this.creditLimit = creditLimit;
        this.availableCredit = availableCredit;
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

    public boolean isIsGstVerified() {
        return isGstVerified;
    }

    public void setIsGstVerified(boolean isGstVerified) {
        this.isGstVerified = isGstVerified;
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
