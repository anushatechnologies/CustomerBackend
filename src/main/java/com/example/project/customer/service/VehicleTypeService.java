package com.example.project.customer.service;

import com.example.project.customer.dto.VehicleTypeRequest;
import com.example.project.customer.dto.VehicleTypeResponse;
import com.example.project.customer.entity.VehicleType;

import java.math.BigDecimal;
import java.util.List;

public interface VehicleTypeService {

    VehicleTypeResponse createVehicleType(VehicleTypeRequest request);

    List<VehicleTypeResponse> getAllVehicleTypes();

    List<VehicleTypeResponse> getActiveVehicleTypes();

    VehicleTypeResponse getVehicleTypeById(Integer id);

    VehicleTypeResponse getVehicleTypeByCode(String code);

    VehicleTypeResponse updateVehicleType(Integer id, VehicleTypeRequest request);

    void deleteVehicleType(Integer id);

    BigDecimal calculateFare(VehicleType vehicleType, double distanceKm);

    VehicleType findEntityByCode(String code);
}
