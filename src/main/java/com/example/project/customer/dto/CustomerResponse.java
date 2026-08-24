package com.example.project.customer.dto;

public class CustomerResponse {

    private final Integer customerId;
    private final String name;
    private final String email;
    private final String phone;

    public CustomerResponse(Integer customerId, String name, String email, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
