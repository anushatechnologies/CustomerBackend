package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    private String name;

    private String contactPerson;
    private String phone;
    private String city;
    private String state;
    private String pincode;
    private String address;
    private Integer capacityTons;

    @JsonProperty("isDefault")
    @Builder.Default
    private Boolean isDefault = false;

    private String status;

    public Boolean isDefault() {
        return isDefault != null && isDefault;
    }
}
