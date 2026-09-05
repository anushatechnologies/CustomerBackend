package com.example.project.customer.service;

import com.example.project.customer.dto.RentalAvailabilityResponse;
import com.example.project.customer.dto.RentalBookingRequest;
import com.example.project.customer.dto.RentalBookingResponse;
import com.example.project.customer.dto.RentalEquipmentResponse;

import java.time.LocalDate;
import java.util.List;

public interface RentalService {
    List<RentalEquipmentResponse> getEquipment(String category, String location, Boolean available);
    RentalEquipmentResponse getEquipmentById(Integer id);
    RentalAvailabilityResponse checkAvailability(Integer id, LocalDate startDate, LocalDate endDate);
    RentalBookingResponse bookEquipment(Integer userId, RentalBookingRequest request);
}
