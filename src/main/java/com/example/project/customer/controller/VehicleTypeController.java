package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.VehicleTypeResponse;
import com.example.project.customer.entity.VehicleType;
import com.example.project.customer.service.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @GetMapping({"/api/seller/vehicle-types", "/api/vehicle-types"})
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> getActiveVehicleTypes() {
        List<VehicleTypeResponse> list = vehicleTypeService.getActiveVehicleTypes();
        return ResponseEntity.ok(ApiResponse.ok("Active vehicle types and fare options retrieved successfully", list));
    }

    @GetMapping("/api/vehicle-types/{code}")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> getVehicleTypeByCode(@PathVariable String code) {
        VehicleTypeResponse response = vehicleTypeService.getVehicleTypeByCode(code);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type details retrieved successfully", response));
    }

    @GetMapping("/api/vehicle-types/{code}/estimate-fare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estimateFare(
            @PathVariable String code,
            @RequestParam(defaultValue = "1.0") double distanceKm
    ) {
        VehicleType vt = vehicleTypeService.findEntityByCode(code);
        BigDecimal fare = vehicleTypeService.calculateFare(vt, distanceKm);

        Map<String, Object> result = new HashMap<>();
        result.put("vehicleTypeCode", vt.getCode());
        result.put("vehicleTypeName", vt.getName());
        result.put("distanceKm", distanceKm);
        result.put("estimatedFare", fare);
        result.put("baseFare", vt.getBaseFare());
        result.put("baseDistanceKm", vt.getBaseDistanceKm());
        result.put("perKmRate", vt.getPerKmRate());
        result.put("minimumFare", vt.getMinimumFare());

        return ResponseEntity.ok(ApiResponse.ok("Fare estimated successfully", result));
    }
}
