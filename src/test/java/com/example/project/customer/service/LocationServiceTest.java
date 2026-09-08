package com.example.project.customer.service;

import com.example.project.customer.dto.ReverseGeocodeRequest;
import com.example.project.customer.dto.ReverseGeocodeResponse;
import com.example.project.customer.exception.GeocodingException;
import com.example.project.customer.service.location.GeocodingClient;
import com.example.project.customer.service.location.LocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private GeocodingClient geocodingClient;

    private LocationServiceImpl locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationServiceImpl(geocodingClient);
    }

    @Test
    @DisplayName("LocationService: Successfully reverse geocodes coordinates")
    void testReverseGeocode_Success() {
        ReverseGeocodeResponse expected = ReverseGeocodeResponse.builder()
                .latitude(17.3850)
                .longitude(78.4866)
                .city("Hyderabad")
                .state("Telangana")
                .country("India")
                .pincode("500001")
                .build();

        when(geocodingClient.reverseGeocode(17.3850, 78.4866)).thenReturn(expected);

        ReverseGeocodeRequest req = ReverseGeocodeRequest.builder()
                .latitude(17.3850)
                .longitude(78.4866)
                .build();

        ReverseGeocodeResponse actual = locationService.reverseGeocode(req);

        assertThat(actual).isNotNull();
        assertThat(actual.getCity()).isEqualTo("Hyderabad");
        assertThat(actual.getPincode()).isEqualTo("500001");
        verify(geocodingClient, times(1)).reverseGeocode(17.3850, 78.4866);
    }

    @Test
    @DisplayName("LocationService: In-memory cache returns cached response without duplicate API calls")
    void testReverseGeocode_Caching() {
        ReverseGeocodeResponse response = ReverseGeocodeResponse.builder()
                .latitude(17.3850)
                .longitude(78.4866)
                .city("Hyderabad")
                .build();

        when(geocodingClient.reverseGeocode(17.3850, 78.4866)).thenReturn(response);

        ReverseGeocodeRequest req = ReverseGeocodeRequest.builder()
                .latitude(17.3850)
                .longitude(78.4866)
                .build();

        // Call twice
        ReverseGeocodeResponse first = locationService.reverseGeocode(req);
        ReverseGeocodeResponse second = locationService.reverseGeocode(req);

        assertThat(first).isSameAs(second);
        // External client should only be called ONCE
        verify(geocodingClient, times(1)).reverseGeocode(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("LocationService: Client failure propagates GeocodingException")
    void testReverseGeocode_Failure() {
        when(geocodingClient.reverseGeocode(17.3850, 78.4866))
                .thenThrow(new GeocodingException("Provider timeout"));

        ReverseGeocodeRequest req = ReverseGeocodeRequest.builder()
                .latitude(17.3850)
                .longitude(78.4866)
                .build();

        assertThatThrownBy(() -> locationService.reverseGeocode(req))
                .isInstanceOf(GeocodingException.class)
                .hasMessageContaining("Provider timeout");
    }

    @Test
    @DisplayName("LocationService: Null request or coordinates throws IllegalArgumentException")
    void testReverseGeocode_NullCheck() {
        assertThatThrownBy(() -> locationService.reverseGeocode(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> locationService.reverseGeocode(new ReverseGeocodeRequest()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
