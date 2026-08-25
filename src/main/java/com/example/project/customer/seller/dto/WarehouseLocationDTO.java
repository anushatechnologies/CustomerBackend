package com.example.project.customer.seller.dto;

import jakarta.validation.constraints.NotBlank;

public class WarehouseLocationDTO {

    @NotBlank(message = "Warehouse state is required")
    private String state;

    @NotBlank(message = "Warehouse city is required")
    private String city;

    @NotBlank(message = "Warehouse address is required")
    private String address;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
