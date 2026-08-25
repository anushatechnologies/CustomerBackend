package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Integer id;
    private String fullName;
    private String phone;
    private String email;
    private String role;
    private String tier;
    private ProcurementStatsDto procurementStats;
    private BusinessDetailsDto business;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Integer id, String fullName, String phone, String email, String role, String tier,
                               ProcurementStatsDto procurementStats, BusinessDetailsDto business) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.tier = tier;
        this.procurementStats = procurementStats;
        this.business = business;
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

    public ProcurementStatsDto getProcurementStats() {
        return procurementStats;
    }

    public void setProcurementStats(ProcurementStatsDto procurementStats) {
        this.procurementStats = procurementStats;
    }

    public BusinessDetailsDto getBusiness() {
        return business;
    }

    public void setBusiness(BusinessDetailsDto business) {
        this.business = business;
    }
}
