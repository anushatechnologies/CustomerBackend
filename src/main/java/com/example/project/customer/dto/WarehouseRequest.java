package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRequest {
    private String name;
    private String contactPerson;
    private String phone;
    private String city;
    private String state;
    private String pincode;
    private String address;
    private Long capacityTons;
    private Boolean isDefault;
}
