package com.example.project.customer.service.location;

import com.example.project.customer.dto.ReverseGeocodeRequest;
import com.example.project.customer.dto.ReverseGeocodeResponse;

public interface LocationService {

    /**
     * Reverse-geocodes given coordinates into a structured location response.
     * Results are cached in memory to avoid duplicate external provider calls.
     *
     * @param request the coordinates request (latitude, longitude)
     * @return populated ReverseGeocodeResponse
     */
    ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request);
}
