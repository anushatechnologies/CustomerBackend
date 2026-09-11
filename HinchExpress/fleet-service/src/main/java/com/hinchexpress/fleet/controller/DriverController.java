package com.hinchexpress.fleet.controller;

import com.hinchexpress.common.dto.ApiResponse;
import com.hinchexpress.common.dto.DriverSummaryDto;
import com.hinchexpress.common.enums.DriverStatus;
import com.hinchexpress.fleet.dto.DriverRegisterRequest;
import com.hinchexpress.fleet.service.DriverService;
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

import java.util.List;

@RestController
@RequestMapping("/api/fleet/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<ApiResponse<DriverSummaryDto>> registerDriver(@Valid @RequestBody DriverRegisterRequest request) {
        DriverSummaryDto created = driverService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Driver registered successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverSummaryDto>> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Driver retrieved successfully", driverService.getDriverById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DriverSummaryDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam DriverStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Driver status updated", driverService.updateDriverStatus(id, status)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<DriverSummaryDto>>> getAvailableDrivers() {
        return ResponseEntity.ok(ApiResponse.ok("Available drivers retrieved", driverService.getAvailableDrivers()));
    }
}
