package com.hinchexpress.fleet.service;

import com.hinchexpress.common.dto.VehicleSummaryDto;
import com.hinchexpress.common.enums.VehicleStatus;
import com.hinchexpress.fleet.dto.VehicleRegisterRequest;
import com.hinchexpress.fleet.entity.Vehicle;

import java.util.List;

public interface VehicleService {

    VehicleSummaryDto registerVehicle(VehicleRegisterRequest request);

    VehicleSummaryDto getVehicleById(Long id);

    VehicleSummaryDto updateVehicleStatus(Long id, VehicleStatus status);

    VehicleSummaryDto updateVehicleLocation(Long id, Double latitude, Double longitude);

    List<VehicleSummaryDto> getAvailableVehicles();

    Vehicle findEntityById(Long id);
}
