package com.example.project.customer.dto;

import com.example.project.customer.entity.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessTaxRequest(

        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 150, message = "Company name must be between 2 and 150 characters")
        String companyName,

        @NotNull(message = "Business type is required (e.g. MANUFACTURER, DISTRIBUTOR, WHOLESALER, RETAILER, DEALER, CONTRACTOR_FABRICATOR, OTHER)")
        BusinessType businessType,

        @NotBlank(message = "GSTIN is required")
        @Pattern(regexp = "^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}[1-9A-Za-z]{1}[Zz][0-9A-Za-z]{1}$", message = "GSTIN must be in valid 15-character format (e.g. 27ABCDE1234F1Z5)")
        String gstin,

        @NotBlank(message = "Business address is required")
        @Size(min = 5, max = 500, message = "Business address must be between 5 and 500 characters")
        String businessAddress,

        @NotBlank(message = "State is required")
        @Pattern(regexp = "^[a-zA-Z\\s]{2,100}$", message = "State must contain only letters and spaces")
        String state,

        @NotBlank(message = "City is required")
        @Pattern(regexp = "^[a-zA-Z\\s]{2,100}$", message = "City must contain only letters and spaces")
        String city,

        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be exactly 6 digits (e.g. 500032)")
        String pincode
) {
}
