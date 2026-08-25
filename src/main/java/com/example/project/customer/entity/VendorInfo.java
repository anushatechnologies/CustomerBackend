package com.example.project.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorInfo {
    private Integer vendorId;
    private String companyName;
    private String city;
    private Boolean isVerified;
    private Double rating;
}
