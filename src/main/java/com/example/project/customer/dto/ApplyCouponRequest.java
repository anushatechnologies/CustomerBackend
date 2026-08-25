package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;

public class ApplyCouponRequest {

    @NotBlank
    private String code;

    public ApplyCouponRequest() {
    }

    public ApplyCouponRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
