package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.RentalAvailabilityResponse;
import com.example.project.customer.dto.RentalBookingRequest;
import com.example.project.customer.dto.RentalBookingResponse;
import com.example.project.customer.dto.RentalEquipmentResponse;
import com.example.project.customer.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RentalEquipmentResponse>>> getEquipment(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean available) {
        List<RentalEquipmentResponse> list = rentalService.getEquipment(category, location, available);
        return ResponseEntity.ok(ApiResponse.ok("Rental equipment catalogue retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RentalEquipmentResponse>> getEquipmentById(@PathVariable Integer id) {
        RentalEquipmentResponse equipment = rentalService.getEquipmentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Equipment details retrieved successfully", equipment));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<RentalAvailabilityResponse>> checkAvailability(
            @PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        RentalAvailabilityResponse availability = rentalService.checkAvailability(id, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Equipment availability checked successfully", availability));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> bookEquipment(
            @Valid @RequestBody RentalBookingRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        RentalBookingResponse booking = rentalService.bookEquipment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Equipment rental booked successfully", booking));
    }
}
