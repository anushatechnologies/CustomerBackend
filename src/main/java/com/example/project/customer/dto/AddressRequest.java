package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class AddressRequest {

    @NotBlank
    private String siteName;

    @NotBlank
    private String recipientName;

    @NotBlank
    private String phone;

    @NotBlank
    private String addressLine1;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String pincode;

    private String landmark;

    @JsonProperty("isDefault")
    private Boolean isDefault = false;

    @JsonProperty("hasHeavyVehicleAccess")
    private Boolean hasHeavyVehicleAccess = true;

    public AddressRequest() {
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

    public Boolean getIsDefault() {
        return isDefault != null ? isDefault : false;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getHasHeavyVehicleAccess() {
        return hasHeavyVehicleAccess != null ? hasHeavyVehicleAccess : true;
    }

    public void setHasHeavyVehicleAccess(Boolean hasHeavyVehicleAccess) {
        this.hasHeavyVehicleAccess = hasHeavyVehicleAccess;
    }
}
