package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.VehicleTypeRequest;
import com.example.project.customer.dto.VehicleTypeResponse;
import com.example.project.customer.service.VehicleTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-types")
@RequiredArgsConstructor
public class AdminVehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> createVehicleType(@Valid @RequestBody VehicleTypeRequest request) {
        VehicleTypeResponse response = vehicleTypeService.createVehicleType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Vehicle type created successfully with fare options", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> getAllVehicleTypes() {
        List<VehicleTypeResponse> list = vehicleTypeService.getAllVehicleTypes();
        return ResponseEntity.ok(ApiResponse.ok("All vehicle types retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> getVehicleTypeById(@PathVariable Integer id) {
        VehicleTypeResponse response = vehicleTypeService.getVehicleTypeById(id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> updateVehicleType(
            @PathVariable Integer id,
            @Valid @RequestBody VehicleTypeRequest request
    ) {
        VehicleTypeResponse response = vehicleTypeService.updateVehicleType(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type and fare options updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicleType(@PathVariable Integer id) {
        vehicleTypeService.deleteVehicleType(id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type deactivated successfully", null));
    }
}
