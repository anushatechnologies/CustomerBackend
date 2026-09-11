package com.hinchexpress.fleet.controller;

import com.hinchexpress.common.dto.ApiResponse;
import com.hinchexpress.fleet.entity.VehicleTier;
import com.hinchexpress.fleet.service.VehicleTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/fleet/tiers")
@RequiredArgsConstructor
public class VehicleTierController {

    private final VehicleTierService vehicleTierService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleTier>>> getAllTiers(
            @RequestParam(required = false) BigDecimal weightKg) {
        List<VehicleTier> tiers = (weightKg != null && weightKg.compareTo(BigDecimal.ZERO) > 0)
                ? vehicleTierService.getEligibleTiers(weightKg)
                : vehicleTierService.getAllTiers();
        return ResponseEntity.ok(ApiResponse.ok("Vehicle tiers retrieved successfully", tiers));
    }
}
