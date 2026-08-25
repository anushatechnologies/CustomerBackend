package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorDto {
    private Integer vendorId;
    private String companyName;
    private String city;
    private boolean isVerified;
    private Double rating;

    public VendorDto() {
    }

    public VendorDto(Integer vendorId, String companyName, String city, boolean isVerified, Double rating) {
        this.vendorId = vendorId;
        this.companyName = companyName;
        this.city = city;
        this.isVerified = isVerified;
        this.rating = rating;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}
