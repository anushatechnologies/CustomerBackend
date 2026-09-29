package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.dto.VehicleTypeRequest;
import com.example.project.customer.dto.VehicleTypeResponse;
import com.example.project.customer.entity.VehicleType;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.VehicleTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({VehicleTypeController.class, AdminVehicleTypeController.class})
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin@hinchmart.com", roles = {"ADMIN", "SELLER"})
public class VehicleTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleTypeService vehicleTypeService;

    @Test
    @DisplayName("Seller GET /api/seller/vehicle-types -> Returns active vehicle types with fare and capacity")
    void testGetActiveVehicleTypesForSeller() throws Exception {
        VehicleTypeResponse vt1 = VehicleTypeResponse.builder()
                .id(1)
                .code("TWO_WHEELER")
                .name("Two Wheeler / Bike")
                .maxWeightKg(BigDecimal.valueOf(20.0))
                .sizeDimensions("40x40x40 cm")
                .baseFare(BigDecimal.valueOf(35.00))
                .baseDistanceKm(2.0)
                .perKmRate(BigDecimal.valueOf(10.00))
                .minimumFare(BigDecimal.valueOf(35.00))
                .fareSummary("₹35 base fare (includes 2.0 km) + ₹10.00/km thereafter")
                .active(true)
                .build();

        VehicleTypeResponse vt2 = VehicleTypeResponse.builder()
                .id(2)
                .code("THREE_WHEELER")
                .name("Three Wheeler / Auto")
                .maxWeightKg(BigDecimal.valueOf(300.0))
                .sizeDimensions("120x90x90 cm")
                .baseFare(BigDecimal.valueOf(80.00))
                .baseDistanceKm(2.0)
                .perKmRate(BigDecimal.valueOf(18.00))
                .minimumFare(BigDecimal.valueOf(80.00))
                .fareSummary("₹80 base fare (includes 2.0 km) + ₹18.00/km thereafter")
                .active(true)
                .build();

        when(vehicleTypeService.getActiveVehicleTypes()).thenReturn(List.of(vt1, vt2));

        mockMvc.perform(get("/api/seller/vehicle-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("TWO_WHEELER"))
                .andExpect(jsonPath("$.data[0].maxWeightKg").value(20.0))
                .andExpect(jsonPath("$.data[1].code").value("THREE_WHEELER"))
                .andExpect(jsonPath("$.data[1].maxWeightKg").value(300.0));
    }

    @Test
    @DisplayName("Admin POST /api/admin/vehicle-types -> Creates vehicle type with fare options")
    void testCreateVehicleTypeByAdmin() throws Exception {
        VehicleTypeRequest request = VehicleTypeRequest.builder()
                .code("TATA_ACE")
                .name("Tata Ace (Chota Hathi)")
                .description("Mini truck for medium loads")
                .maxWeightKg(BigDecimal.valueOf(750.0))
                .sizeDimensions("7x4.5x5 ft")
                .baseFare(BigDecimal.valueOf(250.00))
                .baseDistanceKm(3.0)
                .perKmRate(BigDecimal.valueOf(28.00))
                .minimumFare(BigDecimal.valueOf(250.00))
                .build();

        VehicleTypeResponse response = VehicleTypeResponse.builder()
                .id(3)
                .code("TATA_ACE")
                .name("Tata Ace (Chota Hathi)")
                .maxWeightKg(BigDecimal.valueOf(750.0))
                .baseFare(BigDecimal.valueOf(250.00))
                .baseDistanceKm(3.0)
                .perKmRate(BigDecimal.valueOf(28.00))
                .fareSummary("₹250 base fare (includes 3.0 km) + ₹28.00/km thereafter")
                .active(true)
                .build();

        when(vehicleTypeService.createVehicleType(any(VehicleTypeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/vehicle-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("TATA_ACE"))
                .andExpect(jsonPath("$.data.baseFare").value(250.00));
    }

    @Test
    @DisplayName("GET /api/vehicle-types/{code}/estimate-fare -> Returns dynamically computed fare")
    void testEstimateFareEndpoint() throws Exception {
        VehicleType vt = VehicleType.builder()
                .code("THREE_WHEELER")
                .name("Three Wheeler / Auto")
                .baseFare(BigDecimal.valueOf(80.00))
                .baseDistanceKm(2.0)
                .perKmRate(BigDecimal.valueOf(18.00))
                .minimumFare(BigDecimal.valueOf(80.00))
                .build();

        when(vehicleTypeService.findEntityByCode("THREE_WHEELER")).thenReturn(vt);
        when(vehicleTypeService.calculateFare(eq(vt), eq(5.0))).thenReturn(BigDecimal.valueOf(134.00));

        mockMvc.perform(get("/api/vehicle-types/THREE_WHEELER/estimate-fare?distanceKm=5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleTypeCode").value("THREE_WHEELER"))
                .andExpect(jsonPath("$.data.distanceKm").value(5.0))
                .andExpect(jsonPath("$.data.estimatedFare").value(134.00));
    }
}
