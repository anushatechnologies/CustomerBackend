package com.example.project.customer.seller.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class WarehouseLocation {

    private String state;
    private String city;
    private String address;

    public WarehouseLocation() {
    }

    public WarehouseLocation(String state, String city, String address) {
        this.state = state;
        this.city = city;
        this.address = address;
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
