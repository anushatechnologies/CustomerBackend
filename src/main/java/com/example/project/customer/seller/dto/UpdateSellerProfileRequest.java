package com.example.project.customer.seller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class UpdateSellerProfileRequest {

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Valid
    private AddressDTO address;

    @Valid
    private LegalInfoDTO legal;

    private List<String> serviceAreas = new ArrayList<>();

    private List<WarehouseLocationDTO> warehouseLocations = new ArrayList<>();

    private String country;
    private String state;
    private String city;
    private String pincode;
    private String completeAddress;
    private String gstin;
    private String pan;
    private String cin;
    private String msme;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AddressDTO getAddress() {
        return address;
    }

    public void setAddress(AddressDTO address) {
        this.address = address;
    }

    public LegalInfoDTO getLegal() {
        return legal;
    }

    public void setLegal(LegalInfoDTO legal) {
        this.legal = legal;
    }

    public List<String> getServiceAreas() {
        return serviceAreas;
    }

    public void setServiceAreas(List<String> serviceAreas) {
        this.serviceAreas = serviceAreas == null ? new ArrayList<>() : serviceAreas;
    }

    public List<WarehouseLocationDTO> getWarehouseLocations() {
        return warehouseLocations;
    }

    public void setWarehouseLocations(List<WarehouseLocationDTO> warehouseLocations) {
        this.warehouseLocations = warehouseLocations == null ? new ArrayList<>() : warehouseLocations;
    }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getCompleteAddress() { return completeAddress; }
    public void setCompleteAddress(String completeAddress) { this.completeAddress = completeAddress; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }
    public String getMsme() { return msme; }
    public void setMsme(String msme) { this.msme = msme; }
}
