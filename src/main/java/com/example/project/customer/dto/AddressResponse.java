package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressResponse {

    @JsonProperty("addressId")
    private Integer id;

    private String siteName;
    private String recipientName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String houseFlatNo;
    private String areaLocality;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String landmark;

    private Double latitude;
    private Double longitude;
    private String addressType;

    @JsonProperty("isDefault")
    private boolean isDefault;

    @JsonProperty("hasHeavyVehicleAccess")
    private boolean hasHeavyVehicleAccess;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
