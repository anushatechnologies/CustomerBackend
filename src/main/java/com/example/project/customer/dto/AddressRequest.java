package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @Size(max = 255, message = "Site name cannot exceed 255 characters")
    private String siteName;

    @Size(max = 255, message = "Recipient name cannot exceed 255 characters")
    private String recipientName;

    @Pattern(regexp = "^[0-9+ -]{7,20}$", message = "Phone number must be valid (7-20 digits)")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
    private String addressLine2;

    @Size(max = 255, message = "House or flat number cannot exceed 255 characters")
    private String houseFlatNo;

    @Size(max = 255, message = "Area or locality cannot exceed 255 characters")
    private String areaLocality;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be a valid 6-digit postal code")
    private String pincode;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    @Builder.Default
    private String country = "India";

    @Size(max = 255, message = "Landmark cannot exceed 255 characters")
    private String landmark;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Pattern(regexp = "^(?i)(HOME|WORK|OTHER)$", message = "Address type must be HOME, WORK, or OTHER")
    @Builder.Default
    private String addressType = "OTHER";

    @Builder.Default
    @JsonProperty("isDefault")
    private Boolean isDefault = false;

    @Builder.Default
    @JsonProperty("hasHeavyVehicleAccess")
    private Boolean hasHeavyVehicleAccess = true;
}
