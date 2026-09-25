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
@RequestMapping({"/api/location", "/api/locations"})
public class LocationController {

    private final LocationService locationService;
    private final com.example.project.customer.service.location.ServiceabilityService serviceabilityService;

    public LocationController(
            LocationService locationService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.example.project.customer.service.location.ServiceabilityService serviceabilityService) {
        this.locationService = locationService;
        this.serviceabilityService = serviceabilityService != null ? serviceabilityService : new com.example.project.customer.service.location.ServiceabilityServiceImpl();
    }

    @PostMapping("/reverse-geocode")
    public ResponseEntity<ApiResponse<ReverseGeocodeResponse>> reverseGeocode(
            @Valid @RequestBody ReverseGeocodeRequest request) {
        log.info("Received reverse geocode request for coordinates: lat={}, lon={}",
                request.getLatitude(), request.getLongitude());

        ReverseGeocodeResponse response = locationService.reverseGeocode(request);
        return ResponseEntity.ok(ApiResponse.ok("Location resolved successfully", response));
    }

    @org.springframework.web.bind.annotation.GetMapping("/pincode/{pincode}")
    public ResponseEntity<com.example.project.customer.dto.PincodeServiceabilityResponse> getPincodeDetails(
            @org.springframework.web.bind.annotation.PathVariable String pincode) {
        log.info("Pincode lookup requested for pincode: {}", pincode);
        com.example.project.customer.dto.PincodeServiceabilityResponse response = serviceabilityService.checkPincode(pincode);
        response.setStatusCode(200);
        response.setMessage("Pincode details retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.GetMapping("/serviceability/check")
    public ResponseEntity<com.example.project.customer.dto.PincodeServiceabilityResponse> checkServiceability(
            @org.springframework.web.bind.annotation.RequestParam("pincode") String pincode) {
        log.info("Serviceability check requested for pincode: {}", pincode);
        com.example.project.customer.dto.PincodeServiceabilityResponse response = serviceabilityService.checkPincode(pincode);
        response.setStatusCode(200);
        response.setMessage("Serviceability checked successfully");
        return ResponseEntity.ok(response);
    }
}
