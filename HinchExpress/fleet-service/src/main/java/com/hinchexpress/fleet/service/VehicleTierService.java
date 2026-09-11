package com.hinchexpress.fleet.service;

import com.hinchexpress.fleet.entity.VehicleTier;

import java.math.BigDecimal;
import java.util.List;

public interface VehicleTierService {

    List<VehicleTier> getAllTiers();

    List<VehicleTier> getEligibleTiers(BigDecimal weightKg);
}
