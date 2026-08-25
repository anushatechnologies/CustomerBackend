package com.example.project.customer.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AddressDTO {

    @NotBlank(message = "Country must not be blank")
    private String country;

    @NotBlank(message = "State must not be blank")
    private String state;

    @NotBlank(message = "City must not be blank")
    private String city;

    @NotBlank(message = "Pincode must not be blank")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be a valid 6-digit value")
    private String pincode;

    @NotBlank(message = "Complete address must not be blank")
    private String completeAddress;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getCompleteAddress() {
        return completeAddress;
    }

    public void setCompleteAddress(String completeAddress) {
        this.completeAddress = completeAddress;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
