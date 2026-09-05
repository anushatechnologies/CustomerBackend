package com.example.project.customer.service;

import com.example.project.customer.dto.RentalAvailabilityResponse;
import com.example.project.customer.dto.RentalBookingRequest;
import com.example.project.customer.dto.RentalBookingResponse;
import com.example.project.customer.dto.RentalEquipmentResponse;
import com.example.project.customer.entity.RentalBooking;
import com.example.project.customer.entity.RentalEquipment;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.RentalBookingRepository;
import com.example.project.customer.repository.RentalEquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class RentalServiceImpl implements RentalService {

    private final RentalEquipmentRepository equipmentRepository;
    private final RentalBookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RentalEquipmentResponse> getEquipment(String category, String location, Boolean available) {
        String cleanCat = (category != null && !category.isBlank()) ? category.trim() : null;
        String cleanLoc = (location != null && !location.isBlank()) ? location.trim() : null;

        List<RentalEquipment> list = equipmentRepository.filterEquipment(cleanCat, cleanLoc, available);
        return list.stream().map(this::mapToEquipmentResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RentalEquipmentResponse getEquipmentById(Integer id) {
        RentalEquipment eq = findEquipment(id);
        return mapToEquipmentResponse(eq);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalAvailabilityResponse checkAvailability(Integer id, LocalDate startDate, LocalDate endDate) {
        RentalEquipment eq = findEquipment(id);
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(7);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        List<RentalBooking> conflicts = bookingRepository.findConflictingBookings(id, start, end);
        boolean isAvailable = conflicts.isEmpty() && Boolean.TRUE.equals(eq.getAvailable());

        List<RentalAvailabilityResponse.UnavailableSlot> slots = conflicts.stream()
                .map(c -> RentalAvailabilityResponse.UnavailableSlot.builder()
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .status(c.getStatus())
                        .build())
                .toList();

        return RentalAvailabilityResponse.builder()
                .equipmentId(eq.getEquipmentId())
                .equipmentName(eq.getName())
                .isAvailable(isAvailable)
                .queriedStartDate(start)
                .queriedEndDate(end)
                .bookedSlots(slots)
                .build();
    }

    @Override
    public RentalBookingResponse bookEquipment(Integer userId, RentalBookingRequest request) {
        int uid = userId != null ? userId : 101;
        RentalEquipment eq = findEquipment(request.getEquipmentId());

        if (!Boolean.TRUE.equals(eq.getAvailable())) {
            throw new ResourceConflictException("Equipment is currently under maintenance or unavailable for booking");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Booking end date cannot be before start date");
        }

        List<RentalBooking> conflicts = bookingRepository.findConflictingBookings(
                eq.getEquipmentId(), request.getStartDate(), request.getEndDate());
        if (!conflicts.isEmpty()) {
            throw new ResourceConflictException("Equipment is already booked for the selected date range");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        BigDecimal dailyRate = eq.getDailyRate();
        BigDecimal equipmentCost = dailyRate.multiply(BigDecimal.valueOf(days));

        boolean operatorReq = Boolean.TRUE.equals(request.getOperatorRequired()) && Boolean.TRUE.equals(eq.getOperatorAvailable());
        BigDecimal opDaily = eq.getOperatorDailyCharge() != null ? eq.getOperatorDailyCharge() : BigDecimal.ZERO;
        BigDecimal operatorCost = operatorReq ? opDaily.multiply(BigDecimal.valueOf(days)) : BigDecimal.ZERO;

        BigDecimal deposit = eq.getDepositAmount() != null ? eq.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal totalCost = equipmentCost.add(operatorCost).add(deposit);

        RentalBooking booking = RentalBooking.builder()
                .userId(uid)
                .equipment(eq)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .siteAddress(request.getSiteAddress())
                .operatorRequired(operatorReq)
                .totalDays((int) days)
                .ratePerDay(dailyRate)
                .operatorCost(operatorCost)
                .depositAmount(deposit)
                .totalCost(totalCost)
                .status("CONFIRMED")
                .build();

        RentalBooking saved = bookingRepository.save(booking);
        return mapToBookingResponse(saved);
    }

    private RentalEquipment findEquipment(Integer id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental equipment not found with id: " + id));
    }

    private RentalEquipmentResponse mapToEquipmentResponse(RentalEquipment e) {
        return RentalEquipmentResponse.builder()
                .equipmentId(e.getEquipmentId())
                .name(e.getName())
                .category(e.getCategory())
                .model(e.getModel())
                .specifications(e.getSpecifications())
                .dailyRate(e.getDailyRate())
                .weeklyRate(e.getWeeklyRate())
                .monthlyRate(e.getMonthlyRate())
                .depositAmount(e.getDepositAmount())
                .imageUrl(e.getImageUrl())
                .location(e.getLocation())
                .operatorAvailable(e.getOperatorAvailable())
                .operatorDailyCharge(e.getOperatorDailyCharge())
                .available(e.getAvailable())
                .build();
    }

    private RentalBookingResponse mapToBookingResponse(RentalBooking b) {
        return RentalBookingResponse.builder()
                .bookingId(b.getBookingId())
                .userId(b.getUserId())
                .equipmentId(b.getEquipment().getEquipmentId())
                .equipmentName(b.getEquipment().getName())
                .category(b.getEquipment().getCategory())
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .siteAddress(b.getSiteAddress())
                .operatorRequired(b.getOperatorRequired())
                .totalDays(b.getTotalDays())
                .ratePerDay(b.getRatePerDay())
                .operatorCost(b.getOperatorCost())
                .depositAmount(b.getDepositAmount())
                .totalCost(b.getTotalCost())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
