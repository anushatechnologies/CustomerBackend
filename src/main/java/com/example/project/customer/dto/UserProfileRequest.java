package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {
    private String fullName;
    private String phone;
    private String email;
    private String companyName;
    private String gstNumber;
    private String panNumber;
    private String businessType;
    private BigDecimal creditLimit;
}
