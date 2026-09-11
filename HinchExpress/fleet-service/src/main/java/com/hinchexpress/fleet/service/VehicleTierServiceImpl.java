package com.hinchexpress.fleet.service;

import com.hinchexpress.fleet.entity.VehicleTier;
import com.hinchexpress.fleet.repository.VehicleTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleTierServiceImpl implements VehicleTierService {

    private final VehicleTierRepository vehicleTierRepository;

    @Override
    public List<VehicleTier> getAllTiers() {
        return vehicleTierRepository.findByActiveTrueOrderByMaxPayloadKgAsc();
    }

    @Override
    public List<VehicleTier> getEligibleTiers(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return getAllTiers();
        }
        return vehicleTierRepository.findEligibleTiersForWeight(weightKg);
    }
}
