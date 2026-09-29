package com.example.project.customer.service;

import com.example.project.customer.dto.VehicleTypeRequest;
import com.example.project.customer.dto.VehicleTypeResponse;
import com.example.project.customer.entity.VehicleType;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;

    @Override
    @Transactional
    public VehicleTypeResponse createVehicleType(VehicleTypeRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();
        if (vehicleTypeRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new ResourceConflictException("Vehicle type with code '" + cleanCode + "' already exists");
        }

        VehicleType vt = VehicleType.builder()
                .code(cleanCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .maxWeightKg(request.getMaxWeightKg())
                .sizeDimensions(request.getSizeDimensions())
                .baseFare(request.getBaseFare())
                .baseDistanceKm(request.getBaseDistanceKm())
                .perKmRate(request.getPerKmRate())
                .minimumFare(request.getMinimumFare())
                .active(request.getActive() == null || request.getActive())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .imageUrl(request.getImageUrl())
                .build();

        VehicleType saved = vehicleTypeRepository.save(vt);
        log.info("Created new VehicleType #{} ('{}' - '{}')", saved.getId(), saved.getCode(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getAllVehicleTypes() {
        return vehicleTypeRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getActiveVehicleTypes() {
        return vehicleTypeRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeResponse getVehicleTypeById(Integer id) {
        VehicleType vt = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + id));
        return mapToResponse(vt);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeResponse getVehicleTypeByCode(String code) {
        VehicleType vt = findEntityByCode(code);
        return mapToResponse(vt);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleType findEntityByCode(String code) {
        if (code == null || code.isBlank()) return null;
        return vehicleTypeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with code: " + code));
    }

    @Override
    @Transactional
    public VehicleTypeResponse updateVehicleType(Integer id, VehicleTypeRequest request) {
        VehicleType vt = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + id));

        String cleanCode = request.getCode().trim().toUpperCase();
        if (!vt.getCode().equalsIgnoreCase(cleanCode) && vehicleTypeRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new ResourceConflictException("Vehicle type with code '" + cleanCode + "' already exists");
        }

        vt.setCode(cleanCode);
        vt.setName(request.getName().trim());
        vt.setDescription(request.getDescription());
        vt.setMaxWeightKg(request.getMaxWeightKg());
        vt.setSizeDimensions(request.getSizeDimensions());
        vt.setBaseFare(request.getBaseFare());
        vt.setBaseDistanceKm(request.getBaseDistanceKm());
        vt.setPerKmRate(request.getPerKmRate());
        vt.setMinimumFare(request.getMinimumFare());
        if (request.getActive() != null) {
            vt.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            vt.setSortOrder(request.getSortOrder());
        }
        if (request.getImageUrl() != null) {
            vt.setImageUrl(request.getImageUrl());
        }

        VehicleType updated = vehicleTypeRepository.save(vt);
        log.info("Updated VehicleType #{} ('{}')", updated.getId(), updated.getCode());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteVehicleType(Integer id) {
        VehicleType vt = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + id));
        vt.setActive(false);
        vehicleTypeRepository.save(vt);
        log.info("Deactivated VehicleType #{} ('{}')", id, vt.getCode());
    }

    @Override
    public BigDecimal calculateFare(VehicleType vehicleType, double distanceKm) {
        if (vehicleType == null) {
            return BigDecimal.valueOf(40.0 + (distanceKm * 12.0)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal baseFare = vehicleType.getBaseFare() != null ? vehicleType.getBaseFare() : BigDecimal.valueOf(40.00);
        double baseDist = vehicleType.getBaseDistanceKm();
        BigDecimal perKm = vehicleType.getPerKmRate() != null ? vehicleType.getPerKmRate() : BigDecimal.valueOf(12.00);
        BigDecimal minFare = vehicleType.getMinimumFare() != null ? vehicleType.getMinimumFare() : baseFare;

        BigDecimal fare = baseFare;
        if (distanceKm > baseDist) {
            double extraKm = distanceKm - baseDist;
            BigDecimal extraCharge = perKm.multiply(BigDecimal.valueOf(extraKm));
            fare = fare.add(extraCharge);
        }

        if (fare.compareTo(minFare) < 0) {
            fare = minFare;
        }

        return fare.setScale(2, RoundingMode.HALF_UP);
    }

    private VehicleTypeResponse mapToResponse(VehicleType vt) {
        String fareSummary = String.format("₹%s for first %.1f km, then ₹%s/km (Min: ₹%s)",
                vt.getBaseFare(), vt.getBaseDistanceKm(), vt.getPerKmRate(), vt.getMinimumFare());

        return VehicleTypeResponse.builder()
                .id(vt.getId())
                .code(vt.getCode())
                .name(vt.getName())
                .description(vt.getDescription())
                .maxWeightKg(vt.getMaxWeightKg())
                .sizeDimensions(vt.getSizeDimensions())
                .baseFare(vt.getBaseFare())
                .baseDistanceKm(vt.getBaseDistanceKm())
                .perKmRate(vt.getPerKmRate())
                .minimumFare(vt.getMinimumFare())
                .active(vt.isActive())
                .sortOrder(vt.getSortOrder())
                .imageUrl(vt.getImageUrl())
                .fareSummary(fareSummary)
                .build();
    }
}
