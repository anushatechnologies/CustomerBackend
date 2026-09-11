package com.hinchexpress.fleet.service;

import com.hinchexpress.common.enums.DriverStatus;
import com.hinchexpress.common.enums.VehicleStatus;
import com.hinchexpress.common.enums.VehicleTierCode;
import com.hinchexpress.common.dto.VehicleCandidateDto;
import com.hinchexpress.fleet.entity.Vehicle;
import com.hinchexpress.fleet.entity.VehicleTier;
import com.hinchexpress.fleet.repository.VehicleRepository;
import com.hinchexpress.fleet.repository.VehicleTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FleetMatchingQueryServiceImpl implements FleetMatchingQueryService {

    private final VehicleTierRepository vehicleTierRepository;
    private final VehicleRepository vehicleRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_MAX_RADIUS_KM = 25.0;

    @Override
    public List<VehicleCandidateDto> findEligibleCandidatesForDispatch(
            BigDecimal orderWeightKg,
            Double pickupLatitude,
            Double pickupLongitude,
            Double maxRadiusKm
    ) {
        double radius = (maxRadiusKm != null && maxRadiusKm > 0) ? maxRadiusKm : DEFAULT_MAX_RADIUS_KM;

        // 1. Resolve eligible vehicle tiers (tiers whose max payload >= order weight)
        List<VehicleTier> eligibleTiers = vehicleTierRepository.findEligibleTiersForWeight(orderWeightKg);
        if (eligibleTiers.isEmpty()) {
            log.warn("No vehicle tiers capable of carrying weight: {} kg", orderWeightKg);
            return List.of();
        }

        List<VehicleTierCode> eligibleTierCodes = eligibleTiers.stream()
                .map(VehicleTier::getCode)
                .toList();

        // 2. Query available vehicles belonging to eligible tiers that have an active driver assigned
        List<Vehicle> availableVehicles = vehicleRepository.findAvailableVehiclesInTiers(
                VehicleStatus.AVAILABLE,
                eligibleTierCodes
        );

        List<VehicleCandidateDto> candidates = new ArrayList<>();

        for (Vehicle v : availableVehicles) {
            // Driver must be active and available
            if (v.getDriver() == null || !v.getDriver().isActive() || v.getDriver().getStatus() != DriverStatus.AVAILABLE) {
                continue;
            }

            Double vLat = v.getCurrentLatitude();
            Double vLng = v.getCurrentLongitude();

            // Distance calculation using Haversine
            double distanceKm = (vLat != null && vLng != null && pickupLatitude != null && pickupLongitude != null)
                    ? calculateHaversineDistance(pickupLatitude, pickupLongitude, vLat, vLng)
                    : 0.0; // Default nearby if GPS coordinates not yet initialized

            if (distanceKm <= radius) {
                candidates.add(VehicleCandidateDto.builder()
                        .vehicleId(v.getId())
                        .vehicleNumber(v.getVehicleNumber())
                        .tierCode(v.getTier().getCode())
                        .tierName(v.getTier().getName())
                        .maxPayloadKg(v.getTier().getMaxPayloadKg())
                        .modelName(v.getModelName())
                        .driverId(v.getDriver().getId())
                        .driverName(v.getDriver().getName())
                        .driverPhone(v.getDriver().getPhone())
                        .driverRating(v.getDriver().getRating())
                        .currentLatitude(vLat)
                        .currentLongitude(vLng)
                        .distanceKmToPickup(distanceKm)
                        .build());
            }
        }

        // 3. Sort candidates: Nearest first, then by smaller payload tier (to preserve heavy flatbeds for heavier orders)
        candidates.sort(Comparator
                .comparing(VehicleCandidateDto::getDistanceKmToPickup)
                .thenComparing(VehicleCandidateDto::getMaxPayloadKg));

        log.info("Found {} eligible vehicle candidate(s) for weight: {} kg within {} km of pickup ({}, {})",
                candidates.size(), orderWeightKg, radius, pickupLatitude, pickupLongitude);

        return candidates;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0; // Round to 2 decimals
    }
}
