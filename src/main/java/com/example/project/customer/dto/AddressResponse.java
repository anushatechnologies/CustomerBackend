package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressResponse {

    private Integer addressId;
    private Integer userId;
    private String siteName;
    private String recipientName;
    private String phone;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private String landmark;

    @JsonProperty("isDefault")
    private boolean isDefault;

    @JsonProperty("hasHeavyVehicleAccess")
    private boolean hasHeavyVehicleAccess;

    private LocalDateTime createdAt;

    public AddressResponse() {
    }

    public AddressResponse(Integer addressId, Integer userId, String siteName, String recipientName,
                           String phone, String addressLine1, String city, String state, String pincode,
                           String landmark, boolean isDefault, boolean hasHeavyVehicleAccess, LocalDateTime createdAt) {
        this.addressId = addressId;
        this.userId = userId;
        this.siteName = siteName;
        this.recipientName = recipientName;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.landmark = landmark;
        this.isDefault = isDefault;
        this.hasHeavyVehicleAccess = hasHeavyVehicleAccess;
        this.createdAt = createdAt;
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public boolean isIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isHasHeavyVehicleAccess() {
        return hasHeavyVehicleAccess;
    }

    public void setHasHeavyVehicleAccess(boolean hasHeavyVehicleAccess) {
        this.hasHeavyVehicleAccess = hasHeavyVehicleAccess;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
