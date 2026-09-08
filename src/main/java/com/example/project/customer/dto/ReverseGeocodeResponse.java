package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReverseGeocodeResponse {

    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    private String addressLine1;
    private String addressLine2;
    private String area;
    private String city;
    private String state;
    private String country;
    private String pincode;
}
