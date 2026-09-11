package com.hinchexpress.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRegisterRequest {

    @NotBlank(message = "Driver name is required")
    private String name;

    @NotBlank(message = "Driver phone is required")
    private String phone;

    private String email;

    @NotBlank(message = "License number is required")
    private String licenseNumber;
}
