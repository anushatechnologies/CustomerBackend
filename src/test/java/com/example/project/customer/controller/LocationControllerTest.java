package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.dto.ReverseGeocodeRequest;
import com.example.project.customer.dto.ReverseGeocodeResponse;
import com.example.project.customer.exception.GeocodingException;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.location.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@SuppressWarnings("null")
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @Test
    @DisplayName("Reverse Geocode: Success returns structured location and 200 OK")
    void testReverseGeocode_Success() throws Exception {
        ReverseGeocodeRequest req = ReverseGeocodeRequest.builder()
                .latitude(17.385044)
                .longitude(78.486671)
                .build();

        ReverseGeocodeResponse resp = ReverseGeocodeResponse.builder()
                .latitude(17.385044)
                .longitude(78.486671)
                .formattedAddress("Road No 2, Banjara Hills, Hyderabad, Telangana 500034, India")
                .addressLine1("Road No 2")
                .area("Banjara Hills")
                .city("Hyderabad")
                .state("Telangana")
                .country("India")
                .pincode("500034")
                .build();

        when(locationService.reverseGeocode(any(ReverseGeocodeRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/location/reverse-geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.latitude").value(17.385044))
                .andExpect(jsonPath("$.data.city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.pincode").value("500034"));
    }

    @Test
    @DisplayName("Reverse Geocode: Invalid latitude (> 90.0) returns 400 Bad Request")
    void testReverseGeocode_InvalidLatitude() throws Exception {
        ReverseGeocodeRequest req = ReverseGeocodeRequest.builder()
                .latitude(95.5)
                .longitude(78.486671)
                .build();

        mockMvc.perform(post("/api/location/reverse-geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Reverse Geocode: Invalid longitude (> 180.0) returns 400 Bad Request")
    void testReverseGeocode_InvalidLongitude() throws Exception {
        ReverseGeocodeRequest req = ReverseGeocodeRequest.builder()
                .latitude(17.385044)
                .longitude(195.0)
                .build();

        mockMvc.perform(post("/api/location/reverse-geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Reverse Geocode: Missing coordinates returns 400 Bad Request")
    void testReverseGeocode_NullCoordinates() throws Exception {
        mockMvc.perform(post("/api/location/reverse-geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Reverse Geocode: Service failure returns 503 Service Unavailable")
    void testReverseGeocode_ServiceFailure() throws Exception {
        ReverseGeocodeRequest req = ReverseGeocodeRequest.builder()
                .latitude(17.385044)
                .longitude(78.486671)
                .build();

        when(locationService.reverseGeocode(any(ReverseGeocodeRequest.class)))
                .thenThrow(new GeocodingException("Failed to reverse-geocode coordinates. Please enter address manually."));

        mockMvc.perform(post("/api/location/reverse-geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to reverse-geocode coordinates. Please enter address manually."));
    }
}
