package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Integer id;
    private String fullName;
    private String phone;
    private String email;
    private String role;
    private String tier;

    @JsonProperty("isProfileComplete")
    private boolean profileComplete;

    private ProcurementStats procurementStats;
    private BusinessDetails business;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcurementStats {
        private int totalOrders;
        private int activeRfqs;
        private int wishlistItems;
        private int savedAddresses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessDetails {
        private String companyName;
        private String gstNumber;
        private String panNumber;
        private String businessType;

        @JsonProperty("isGstVerified")
        private boolean gstVerified;

        private BigDecimal creditLimit;
        private BigDecimal availableCredit;
    }
}
