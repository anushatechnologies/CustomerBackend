package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {
    private String id;
    private String name;
    private Boolean isDefault;
    private String contactPerson;
    private String phone;
    private String city;
    private String state;
    private String pincode;
    private String address;
    private Long capacityTons;
    private Long currentLoadTons;
    private String status;
    private LocalDateTime createdAt;
}
