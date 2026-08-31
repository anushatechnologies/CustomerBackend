package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BankDetailsRequest(

        @NotBlank(message = "Bank name is required")
        @Pattern(regexp = "^[a-zA-Z\\s.]{2,100}$", message = "Bank name must contain only letters and spaces")
        String bankName,

        @NotBlank(message = "Account holder name is required")
        @Pattern(regexp = "^[a-zA-Z\\s.]{2,100}$", message = "Account holder name must contain only letters and spaces")
        String accountHolderName,

        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^[0-9]{9,18}$", message = "Account number must be between 9 and 18 numeric digits")
        String accountNumber,

        @NotBlank(message = "Confirm account number is required")
        String confirmAccountNumber,

        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "IFSC code must be in valid 11-character format (e.g. HDFC0001234)")
        String ifscCode,

        @NotBlank(message = "Account type is required")
        @Pattern(regexp = "^(?i)(SAVINGS|CURRENT)$", message = "Account type must be either SAVINGS or CURRENT")
        String accountType
) {
}
