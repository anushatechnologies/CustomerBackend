package com.hinchexpress.fleet.controller;

import com.hinchexpress.common.dto.ApiResponse;
import com.hinchexpress.common.dto.VehicleSummaryDto;
import com.hinchexpress.common.enums.VehicleStatus;
import com.hinchexpress.common.dto.VehicleCandidateDto;
import com.hinchexpress.fleet.dto.VehicleRegisterRequest;
import com.hinchexpress.fleet.service.FleetMatchingQueryService;
import com.hinchexpress.fleet.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/fleet/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final FleetMatchingQueryService fleetMatchingQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleSummaryDto>> registerVehicle(@Valid @RequestBody VehicleRegisterRequest request) {
        VehicleSummaryDto created = vehicleService.registerVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Vehicle registered successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleSummaryDto>> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vehicle retrieved successfully", vehicleService.getVehicleById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VehicleSummaryDto>> updateVehicleStatus(
            @PathVariable Long id,
            @RequestParam VehicleStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Vehicle status updated", vehicleService.updateVehicleStatus(id, status)));
    }

    @PatchMapping("/{id}/location")
    public ResponseEntity<ApiResponse<VehicleSummaryDto>> updateVehicleLocation(
            @PathVariable Long id,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        return ResponseEntity.ok(ApiResponse.ok("Vehicle coordinates updated", vehicleService.updateVehicleLocation(id, latitude, longitude)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VehicleSummaryDto>>> getAvailableVehicles() {
        return ResponseEntity.ok(ApiResponse.ok("Available vehicles retrieved", vehicleService.getAvailableVehicles()));
    }

    /**
     * Internal REST API queried by dispatch-service to evaluate eligible vehicle candidates for an order.
     */
    @GetMapping("/eligible-for-dispatch")
    public ResponseEntity<ApiResponse<List<VehicleCandidateDto>>> getEligibleCandidates(
            @RequestParam BigDecimal weightKg,
            @RequestParam Double pickupLatitude,
            @RequestParam Double pickupLongitude,
            @RequestParam(required = false, defaultValue = "25.0") Double maxRadiusKm) {
        List<VehicleCandidateDto> candidates = fleetMatchingQueryService.findEligibleCandidatesForDispatch(
                weightKg,
                pickupLatitude,
                pickupLongitude,
                maxRadiusKm
        );
        return ResponseEntity.ok(ApiResponse.ok("Eligible vehicle candidates retrieved for dispatch", candidates));
    }
}
