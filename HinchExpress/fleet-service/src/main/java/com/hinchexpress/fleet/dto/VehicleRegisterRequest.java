package com.hinchexpress.fleet.dto;

import com.hinchexpress.common.enums.VehicleTierCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegisterRequest {

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    @NotNull(message = "Vehicle tier code is required")
    private VehicleTierCode tierCode;

    @NotBlank(message = "Model name is required")
    private String modelName;

    private Long driverId;

    private Double initialLatitude;

    private Double initialLongitude;
}
