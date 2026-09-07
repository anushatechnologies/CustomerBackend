package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ReverseGeocodeRequest;
import com.example.project.customer.dto.ReverseGeocodeResponse;
import com.example.project.customer.service.location.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/reverse-geocode")
    public ResponseEntity<ApiResponse<ReverseGeocodeResponse>> reverseGeocode(
            @Valid @RequestBody ReverseGeocodeRequest request) {
        log.info("Received reverse geocode request for coordinates: lat={}, lon={}",
                request.getLatitude(), request.getLongitude());

        ReverseGeocodeResponse response = locationService.reverseGeocode(request);
        return ResponseEntity.ok(ApiResponse.ok("Location resolved successfully", response));
    }
}
