package com.example.project.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PersonalKycRequest(

        @NotBlank(message = "Name is required")
        @Pattern(regexp = "^[a-zA-Z\\s]{2,100}$", message = "Name must contain only alphabets and spaces (2 to 100 characters)")
        String name,

        @NotBlank(message = "Email is required")
        @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email must be a valid email address (e.g. name@domain.com)")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(?:\\+91)?[6-9][0-9]{9}$", message = "Phone must be a valid 10-digit Indian mobile number (e.g. 9876543210 or +919876543210)")
        String phone,

        @NotBlank(message = "PAN number is required")
        @Pattern(regexp = "^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$", message = "PAN must be in valid 10-character alphanumeric format (e.g. ABCDE1234F)")
        String panNumber,

        @NotBlank(message = "Aadhaar number is required")
        @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar must be exactly 12 numeric digits")
        String aadhaarNumber
) {
}
