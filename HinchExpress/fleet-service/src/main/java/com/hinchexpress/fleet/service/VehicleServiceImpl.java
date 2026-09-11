package com.hinchexpress.fleet.service;

import com.hinchexpress.common.dto.VehicleSummaryDto;
import com.hinchexpress.common.enums.VehicleStatus;
import com.hinchexpress.fleet.dto.VehicleRegisterRequest;
import com.hinchexpress.fleet.entity.Driver;
import com.hinchexpress.fleet.entity.Vehicle;
import com.hinchexpress.fleet.entity.VehicleTier;
import com.hinchexpress.fleet.repository.DriverRepository;
import com.hinchexpress.fleet.repository.VehicleRepository;
import com.hinchexpress.fleet.repository.VehicleTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTierRepository vehicleTierRepository;
    private final DriverRepository driverRepository;

    @Override
    public VehicleSummaryDto registerVehicle(VehicleRegisterRequest request) {
        if (vehicleRepository.findByVehicleNumber(request.getVehicleNumber()).isPresent()) {
            throw new IllegalArgumentException("Vehicle with registration number " + request.getVehicleNumber() + " already exists.");
        }

        VehicleTier tier = vehicleTierRepository.findById(request.getTierCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid vehicle tier code: " + request.getTierCode()));

        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + request.getDriverId()));
        }

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(request.getVehicleNumber().trim().toUpperCase())
                .tier(tier)
                .driver(driver)
                .modelName(request.getModelName().trim())
                .status(VehicleStatus.AVAILABLE)
                .currentLatitude(request.getInitialLatitude())
                .currentLongitude(request.getInitialLongitude())
                .lastPingAt(request.getInitialLatitude() != null ? LocalDateTime.now() : null)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Registered vehicle: {} [Tier: {}] (ID: {})", saved.getVehicleNumber(), saved.getTier().getName(), saved.getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleSummaryDto getVehicleById(Long id) {
        return mapToDto(findEntityById(id));
    }

    @Override
    public VehicleSummaryDto updateVehicleStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = findEntityById(id);
        vehicle.setStatus(status);
        vehicle.setUpdatedAt(LocalDateTime.now());
        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Updated vehicle #{} status to {}", id, status);
        return mapToDto(updated);
    }

    @Override
    public VehicleSummaryDto updateVehicleLocation(Long id, Double latitude, Double longitude) {
        Vehicle vehicle = findEntityById(id);
        vehicle.setCurrentLatitude(latitude);
        vehicle.setCurrentLongitude(longitude);
        vehicle.setLastPingAt(LocalDateTime.now());
        Vehicle updated = vehicleRepository.save(vehicle);
        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleSummaryDto> getAvailableVehicles() {
        return vehicleRepository.findByStatusAndActiveTrue(VehicleStatus.AVAILABLE)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Vehicle findEntityById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with id: " + id));
    }

    private VehicleSummaryDto mapToDto(Vehicle v) {
        return VehicleSummaryDto.builder()
                .id(v.getId())
                .vehicleNumber(v.getVehicleNumber())
                .tierCode(v.getTier().getCode())
                .tierName(v.getTier().getName())
                .modelName(v.getModelName())
                .driverId(v.getDriver() != null ? v.getDriver().getId() : null)
                .driverName(v.getDriver() != null ? v.getDriver().getName() : null)
                .driverPhone(v.getDriver() != null ? v.getDriver().getPhone() : null)
                .status(v.getStatus())
                .currentLatitude(v.getCurrentLatitude())
                .currentLongitude(v.getCurrentLongitude())
                .lastPingAt(v.getLastPingAt())
                .active(v.isActive())
                .build();
    }
}
