package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.AddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@SuppressWarnings("null")
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddressService addressService;

    @MockBean
    private UserContextUtil userContextUtil;

    @Test
    @DisplayName("Create Address: Manual address without coordinates returns 201 Created")
    void testCreateAddress_Manual_Success() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        AddressRequest request = AddressRequest.builder()
                .recipientName("Pavan Kumar")
                .phone("9876543210")
                .houseFlatNo("Flat 402, Sai Residency")
                .addressLine1("Gachibowli Main Road")
                .areaLocality("Gachibowli")
                .city("Hyderabad")
                .state("Telangana")
                .country("India")
                .pincode("500032")
                .addressType("HOME")
                .isDefault(true)
                .build();

        AddressResponse response = AddressResponse.builder()
                .id(1)
                .siteName("HOME Address")
                .recipientName("Pavan Kumar")
                .phone("9876543210")
                .houseFlatNo("Flat 402, Sai Residency")
                .addressLine1("Gachibowli Main Road")
                .areaLocality("Gachibowli")
                .city("Hyderabad")
                .state("Telangana")
                .country("India")
                .pincode("500032")
                .addressType("HOME")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(addressService.createAddress(eq(101), any(AddressRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addressId").value(1))
                .andExpect(jsonPath("$.data.city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    @DisplayName("Create Address: GPS current location with coordinates returns 201 Created")
    void testCreateAddress_CurrentLocation_Success() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        AddressRequest request = AddressRequest.builder()
                .siteName("Project Alpha Site")
                .recipientName("Site Engineer")
                .phone("9849012345")
                .addressLine1("Road No 10, Banjara Hills")
                .areaLocality("Banjara Hills")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500034")
                .latitude(17.4156)
                .longitude(78.4350)
                .addressType("WORK")
                .build();

        AddressResponse response = AddressResponse.builder()
                .id(2)
                .siteName("Project Alpha Site")
                .addressLine1("Road No 10, Banjara Hills")
                .city("Hyderabad")
                .latitude(17.4156)
                .longitude(78.4350)
                .addressType("WORK")
                .build();

        when(addressService.createAddress(eq(101), any(AddressRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.latitude").value(17.4156));
    }

    @Test
    @DisplayName("Get Addresses: GET /api/addresses returns customer addresses")
    void testGetAddresses() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        AddressResponse a1 = AddressResponse.builder().id(1).city("Hyderabad").isDefault(true).build();
        AddressResponse a2 = AddressResponse.builder().id(2).city("Secunderabad").isDefault(false).build();

        when(addressService.getAddresses(101)).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].addressId").value(1));
    }

    @Test
    @DisplayName("Get Addresses: GET /api/user/addresses backward-compatibility mapping works")
    void testGetAddresses_LegacyUrl() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        when(addressService.getAddresses(101)).thenReturn(List.of());

        mockMvc.perform(get("/api/user/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Get Address by ID: GET /api/addresses/{id} returns address")
    void testGetAddressById() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        AddressResponse resp = AddressResponse.builder().id(1).city("Hyderabad").build();
        when(addressService.getAddressById(101, 1)).thenReturn(resp);

        mockMvc.perform(get("/api/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addressId").value(1));
    }

    @Test
    @DisplayName("Update Address: PUT /api/addresses/{id} updates address")
    void testUpdateAddress() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        AddressRequest request = AddressRequest.builder()
                .addressLine1("Updated Street")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500081")
                .build();

        AddressResponse resp = AddressResponse.builder().id(1).addressLine1("Updated Street").build();
        when(addressService.updateAddress(eq(101), eq(1), any(AddressRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addressLine1").value("Updated Street"));
    }

    @Test
    @DisplayName("Delete Address: DELETE /api/addresses/{id} deletes address")
    void testDeleteAddress() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        doNothing().when(addressService).deleteAddress(101, 1);

        mockMvc.perform(delete("/api/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Set Default Address: PATCH /api/addresses/{id}/default sets address as default")
    void testSetDefaultAddress() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        AddressResponse resp = AddressResponse.builder().id(2).isDefault(true).build();
        when(addressService.setDefaultAddress(101, 2)).thenReturn(resp);

        mockMvc.perform(patch("/api/addresses/2/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    @DisplayName("Validation: Invalid pincode format (e.g. '123') returns 400 Bad Request")
    void testCreateAddress_InvalidPincode() throws Exception {
        AddressRequest request = AddressRequest.builder()
                .addressLine1("Line 1")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("123") // Invalid pincode
                .build();

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
