package com.hinchexpress.fleet.service;

import com.hinchexpress.common.dto.VehicleCandidateDto;

import java.math.BigDecimal;
import java.util.List;

public interface FleetMatchingQueryService {

    /**
     * Finds available vehicles capable of carrying the given order weight, sorted by proximity to the pickup coordinates.
     */
    List<VehicleCandidateDto> findEligibleCandidatesForDispatch(
            BigDecimal orderWeightKg,
            Double pickupLatitude,
            Double pickupLongitude,
            Double maxRadiusKm
    );
}
