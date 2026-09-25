package com.example.project.customer.controller;

import com.example.project.customer.dto.PincodeServiceabilityResponse;
import com.example.project.customer.service.location.ServiceabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/serviceability")
@RequiredArgsConstructor
public class ServiceabilityController {

    private final ServiceabilityService serviceabilityService;

    @GetMapping("/check")
    public ResponseEntity<PincodeServiceabilityResponse> checkServiceability(
            @RequestParam(value = "pincode", required = false, defaultValue = "") String pincode) {
        log.info("Checking delivery serviceability for pincode: {}", pincode);
        PincodeServiceabilityResponse response = serviceabilityService.checkPincode(pincode);
        response.setStatusCode(200);
        response.setMessage("Serviceability checked successfully");
        return ResponseEntity.ok(response);
    }
}
