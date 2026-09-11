package com.hinchexpress.fleet.service;

import com.hinchexpress.common.dto.DriverSummaryDto;
import com.hinchexpress.common.enums.DriverStatus;
import com.hinchexpress.fleet.dto.DriverRegisterRequest;
import com.hinchexpress.fleet.entity.Driver;
import com.hinchexpress.fleet.repository.DriverRepository;
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
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Override
    public DriverSummaryDto registerDriver(DriverRegisterRequest request) {
        if (driverRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Driver with phone " + request.getPhone() + " is already registered.");
        }

        Driver driver = Driver.builder()
                .name(request.getName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .licenseNumber(request.getLicenseNumber().trim())
                .status(DriverStatus.AVAILABLE)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        Driver saved = driverRepository.save(driver);
        log.info("Registered new driver: {} (ID: {})", saved.getName(), saved.getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverSummaryDto getDriverById(Long id) {
        Driver driver = findEntityById(id);
        return mapToDto(driver);
    }

    @Override
    public DriverSummaryDto updateDriverStatus(Long id, DriverStatus status) {
        Driver driver = findEntityById(id);
        driver.setStatus(status);
        driver.setUpdatedAt(LocalDateTime.now());
        Driver updated = driverRepository.save(driver);
        log.info("Updated driver #{} status to {}", id, status);
        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverSummaryDto> getAvailableDrivers() {
        return driverRepository.findByStatusAndActiveTrue(DriverStatus.AVAILABLE)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Driver findEntityById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + id));
    }

    private DriverSummaryDto mapToDto(Driver d) {
        return DriverSummaryDto.builder()
                .id(d.getId())
                .name(d.getName())
                .phone(d.getPhone())
                .licenseNumber(d.getLicenseNumber())
                .status(d.getStatus())
                .rating(d.getRating())
                .totalTripsCompleted(d.getTotalTripsCompleted())
                .active(d.isActive())
                .build();
    }
}
