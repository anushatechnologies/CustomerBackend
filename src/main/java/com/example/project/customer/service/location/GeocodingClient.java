package com.example.project.customer.service.location;

import com.example.project.customer.dto.ReverseGeocodeResponse;

public interface GeocodingClient {

    /**
     * Reverse geocodes coordinates to a structured location response.
     *
     * @param latitude  the latitude (-90.0 to 90.0)
     * @param longitude the longitude (-180.0 to 180.0)
     * @return populated ReverseGeocodeResponse
     */
    ReverseGeocodeResponse reverseGeocode(Double latitude, Double longitude);
}
