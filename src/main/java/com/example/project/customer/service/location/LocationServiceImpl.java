package com.example.project.customer.service.location;

import com.example.project.customer.dto.ReverseGeocodeRequest;
import com.example.project.customer.dto.ReverseGeocodeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final GeocodingClient geocodingClient;

    // Lightweight in-memory cache to prevent redundant external API calls
    // Coordinates rounded to 4 decimal places (~11 meters precision)
    private final Map<String, ReverseGeocodeResponse> cache = new ConcurrentHashMap<>();

    @Override
    public ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude must not be null");
        }

        String cacheKey = formatCacheKey(request.getLatitude(), request.getLongitude());

        ReverseGeocodeResponse cached = cache.get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for reverse geocoding coordinates: key={}", cacheKey);
            return cached;
        }

        log.info("Cache miss for coordinates ({}, {}). Requesting external geocoding...",
                request.getLatitude(), request.getLongitude());

        ReverseGeocodeResponse response = geocodingClient.reverseGeocode(
                request.getLatitude(),
                request.getLongitude()
        );

        if (response != null) {
            cache.put(cacheKey, response);
        }

        return response;
    }

    private String formatCacheKey(Double lat, Double lon) {
        return String.format(Locale.ROOT, "%.4f,%.4f", lat, lon);
    }
}
