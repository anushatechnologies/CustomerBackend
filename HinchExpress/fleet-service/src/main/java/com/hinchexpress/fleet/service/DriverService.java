package com.hinchexpress.fleet.service;

import com.hinchexpress.common.dto.DriverSummaryDto;
import com.hinchexpress.common.enums.DriverStatus;
import com.hinchexpress.fleet.dto.DriverRegisterRequest;
import com.hinchexpress.fleet.entity.Driver;

import java.util.List;

public interface DriverService {

    DriverSummaryDto registerDriver(DriverRegisterRequest request);

    DriverSummaryDto getDriverById(Long id);

    DriverSummaryDto updateDriverStatus(Long id, DriverStatus status);

    List<DriverSummaryDto> getAvailableDrivers();

    Driver findEntityById(Long id);
}
